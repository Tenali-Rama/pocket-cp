package app.pocketcp.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class RequestQueueTest {
    @Test
    fun `queued API calls start 2 s apart`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { ok() }
            withQueue(upstream) { queue ->
                (1..3).map { async { queue.execute(get("$API/user.info?handles=user$it")) } }.awaitAll()
            }
            assertEquals(listOf(0L, 2_000L, 4_000L), upstream.starts)
        }

    @Test
    fun `page fetches are not spaced`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { ok() }
            withQueue(upstream) { queue ->
                (1..3).map { async { queue.execute(get("$HOST/contest/$it")) } }.awaitAll()
            }
            assertEquals(listOf(0L, 0L, 0L), upstream.starts)
        }

    @Test
    fun `concurrent identical GETs share one upstream call`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }, latencyMs = 1_000) { ok("shared") }
            val results =
                withQueue(upstream) { queue ->
                    List(2) { async { queue.execute(get("$API/contest.list")) } }.awaitAll()
                }
            assertEquals(1, upstream.starts.size)
            assertEquals(listOf("shared", "shared"), results.map { it.body })
        }

    @Test
    fun `cancelling one waiter keeps the shared call for the other`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }, latencyMs = 1_000) { ok("kept") }
            val result =
                withQueue(upstream) { queue ->
                    val first = async { queue.execute(get("$API/contest.list")) }
                    val second = async { queue.execute(get("$API/contest.list")) }
                    advanceTimeBy(500)
                    first.cancel()
                    second.await()
                }
            assertEquals("kept", result.body)
            assertEquals(1, upstream.starts.size)
            assertEquals(1, upstream.completed)
        }

    @Test
    fun `POSTs are never deduplicated`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }, latencyMs = 1_000) { ok() }
            withQueue(upstream) { queue ->
                List(2) { async { queue.execute(post("$HOST/data/submit")) } }.awaitAll()
            }
            assertEquals(2, upstream.starts.size)
        }

    @Test
    fun `429 retries after 2, 4 and 8 s, then fails as RateLimited`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { response(429) }
            val error = withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list")) } }
            assertTrue("got $error", error is NetworkError.RateLimited)
            assertEquals(RETRY_STARTS, upstream.starts)
        }

    @Test
    fun `502, 503 and 504 retry with the same backoff, then fail as Unavailable`() =
        listOf(502, 503, 504).forEach { code ->
            runTest {
                val upstream = FakeUpstream({ currentTime }) { response(code) }
                val error = withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list")) } }
                assertEquals(code, (error as NetworkError.Unavailable).code)
                assertEquals(RETRY_STARTS, upstream.starts)
            }
        }

    @Test
    fun `a retried call succeeds when the server recovers`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { attempt -> if (attempt < 3) response(503) else ok("back") }
            val result = withQueue(upstream) { queue -> queue.execute(get("$API/contest.list")) }
            assertEquals("back", result.body)
            assertEquals(listOf(0L, 2_000L, 6_000L), upstream.starts)
        }

    @Test
    fun `an IOException fails at once as Offline`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { throw IOException("no route") }
            val error = withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list")) } }
            assertTrue("got $error", error is NetworkError.Offline)
            assertEquals(1, upstream.starts.size)
        }

    @Test
    fun `a timeout fails at once as Offline`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { throw SocketTimeoutException("read timed out") }
            val error = withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list")) } }
            assertTrue("got $error", error is NetworkError.Offline && error.cause is SocketTimeoutException)
            assertEquals(1, upstream.starts.size)
        }

    @Test
    fun `a body the classifier marks retryable backs off, then fails with the error it named`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { ok("busy") }
            val classifier =
                RetryClassifier { if (it.body == "busy") RetryDecision.Retry(NetworkError.Rejected("busy")) else RetryDecision.Accept }
            val error =
                withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list"), classifier) } }
            assertEquals("busy", (error as NetworkError.Rejected).reason)
            assertEquals(RETRY_STARTS, upstream.starts)
        }

    @Test
    fun `a classifier Fail decision fails at once`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { ok("handle not found") }
            val classifier = RetryClassifier { RetryDecision.Fail(NetworkError.Rejected(it.body)) }
            val error =
                withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/user.info"), classifier) } }
            assertEquals("handle not found", (error as NetworkError.Rejected).reason)
            assertEquals(1, upstream.starts.size)
        }

    @Test
    fun `the classifier never sees a 5xx outside the retry set and the caller gets it as it is`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { response(500) }
            val classifier = RetryClassifier { error("classifier must not run on a 500") }
            val result = withQueue(upstream) { queue -> queue.execute(get("$HOST/profile/nobody"), classifier) }
            assertEquals(500, result.code)
            assertEquals(1, upstream.starts.size)
        }

    @Test
    fun `a 400 the classifier marks retryable backs off 2, 4 and 8 s, then fails with the error it named`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { response(400, "busy") }
            val error =
                withQueue(upstream) { queue -> failureOf { queue.execute(get("$API/contest.list"), BUSY_RETRIES) } }
            assertEquals("busy", (error as NetworkError.Rejected).reason)
            assertEquals(RETRY_STARTS, upstream.starts)
        }

    @Test
    fun `a retried 400 returns the 2xx response once the server accepts the call`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { attempt -> if (attempt == 1) response(400, "busy") else ok("done") }
            val result = withQueue(upstream) { queue -> queue.execute(get("$API/contest.list"), BUSY_RETRIES) }
            assertEquals(200, result.code)
            assertEquals("done", result.body)
            assertEquals(listOf(0L, 2_000L), upstream.starts)
        }

    @Test
    fun `with AcceptAll a 400 reaches the caller unchanged after one attempt`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { response(400, "busy") }
            val result = withQueue(upstream) { queue -> queue.execute(get("$API/contest.list")) }
            assertEquals(400, result.code)
            assertEquals("busy", result.body)
            assertEquals(1, upstream.starts.size)
        }

    @Test
    fun `plain http requests are rejected`() =
        runTest {
            val upstream = FakeUpstream({ currentTime }) { ok() }
            val error = withQueue(upstream) { queue -> failureOf { queue.execute(get("http://codeforces.com/api/contest.list")) } }
            assertTrue("got $error", error is IllegalArgumentException)
            assertTrue(upstream.starts.isEmpty())
        }

    /** Runs [block] against a queue whose shared scope lives on the test scheduler, then cancels that scope. */
    private suspend fun <T> TestScope.withQueue(
        upstream: HttpTransport,
        block: suspend TestScope.(RequestQueue) -> T,
    ): T {
        val queueScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            return block(RequestQueue(upstream, queueScope))
        } finally {
            queueScope.cancel()
        }
    }

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()

    /** Records when each call starts (virtual ms); [respond] gets the 1-based attempt number. */
    private class FakeUpstream(
        private val clock: () -> Long,
        private val latencyMs: Long = 0,
        private val respond: (Int) -> NetworkResponse,
    ) : HttpTransport {
        val starts = mutableListOf<Long>()
        var completed = 0

        override suspend fun execute(request: Request): NetworkResponse {
            starts += clock()
            val attempt = starts.size
            delay(latencyMs)
            return respond(attempt).also { completed++ }
        }
    }

    private companion object {
        const val HOST = "https://codeforces.com"
        const val API = "$HOST/api"
        val RETRY_STARTS = listOf(0L, 2_000L, 6_000L, 14_000L)

        /** Retries any "busy" body, whatever its status. */
        val BUSY_RETRIES =
            RetryClassifier { if (it.body == "busy") RetryDecision.Retry(NetworkError.Rejected("busy")) else RetryDecision.Accept }

        fun get(url: String) = Request.Builder().url(url).build()

        fun post(url: String) =
            Request
                .Builder()
                .url(url)
                .post("x".toRequestBody())
                .build()

        fun response(
            code: Int,
            body: String = "",
        ) = NetworkResponse(code, Headers.headersOf(), body)

        fun ok(body: String = "{}") = response(200, body)
    }
}
