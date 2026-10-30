package app.pocketcp.core.codeforces

import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CodeforcesQueueTest {
    @Test
    fun `three different calls start 2 s apart because they share the request queue`() =
        runTest {
            val server =
                FixtureServer()
                    .map("/api/contest.list", ApiFixtures.CONTESTS)
                    .map("/api/problemset.problems", ApiFixtures.PROBLEMS)
                    .map("/api/user.info?handles=tourist;jiangly", ApiFixtures.USERS)
            val starts = mutableListOf<Pair<Long, String>>()
            val timed =
                HttpTransport { request ->
                    starts += currentTime to request.url.encodedPath
                    server.execute(request)
                }

            withApi(timed) { api ->
                val contests = async { api.contests() }
                val problems = async { api.problemset() }
                val users = async { api.users(listOf("tourist", "jiangly")) }
                contests.await()
                problems.await()
                users.await()
            }

            val expected =
                listOf(
                    0L to "/api/contest.list",
                    2_000L to "/api/problemset.problems",
                    4_000L to "/api/user.info",
                )
            assertEquals(expected, starts)
        }

    @Test
    fun `call limit exceeded with HTTP 400 is retried 2, 4 and 8 s apart, then becomes RateLimited`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CALL_LIMIT, code = HTTP_BAD_REQUEST)
            val starts = mutableListOf<Long>()
            val timed =
                HttpTransport { request ->
                    starts += currentTime
                    server.execute(request)
                }

            val error = withApi(timed) { api -> failureOf { api.contests() } }

            assertTrue("got $error", error is CodeforcesError.RateLimited)
            assertEquals(listOf(0L, 2_000L, 6_000L, 14_000L), starts)
        }

    @Test
    fun `call limit exceeded with HTTP 400 once, then the contests, returns the contests`() =
        runTest {
            val limited = FixtureServer().map("/api/contest.list", ApiFixtures.CALL_LIMIT, code = HTTP_BAD_REQUEST)
            val healthy = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS)
            var attempts = 0
            val recovering =
                HttpTransport { request ->
                    attempts++
                    if (attempts == 1) limited.execute(request) else healthy.execute(request)
                }

            val contests = withApi(recovering) { api -> api.contests() }

            assertEquals(listOf(2278, 2272, 2277), contests.map { it.id })
            assertEquals(2, attempts)
        }

    @Test
    fun `handle not found with HTTP 400 is not retried and becomes an Api error`() =
        runTest {
            val path = "/api/user.info?handles=no_such_handle_zzzq"
            val server = FixtureServer().map(path, ApiFixtures.HANDLE_NOT_FOUND, code = HTTP_BAD_REQUEST)

            val error = withApi(server) { api -> failureOf { api.users(listOf("no_such_handle_zzzq")) } }

            assertTrue("got $error", error is CodeforcesError.Api)
            assertEquals(1, server.requests.size)
        }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
    }
}
