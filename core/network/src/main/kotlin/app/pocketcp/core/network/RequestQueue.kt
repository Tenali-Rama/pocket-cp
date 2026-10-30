package app.pocketcp.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import okhttp3.Request
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The one way to reach the network (ADR 0003):
 * - `/api/` calls start at least [API_INTERVAL] apart, in arrival order; other requests are not spaced.
 * - Concurrent identical GETs share one upstream call. It runs in [scope], so a cancelled caller never cancels it.
 * - 429 and 502/503/504, and 2xx or 4xx responses the [RetryClassifier] marks retryable, retry after [BACKOFF],
 *   then fail. Codeforces sends "Call limit exceeded" with HTTP 400, so 4xx goes through the classifier too.
 * - Network failures fail at once with [NetworkError.Offline]. Other statuses are returned to the caller as they are.
 */
class RequestQueue(
    private val transport: HttpTransport,
    private val scope: CoroutineScope,
) {
    private val inFlight = mutableMapOf<String, Deferred<NetworkResponse>>()
    private val apiSlot = Mutex()

    /**
     * Runs [request] (HTTPS only) and returns its response.
     * Concurrent GETs of the same URL are deduplicated, so they should pass the same [classifier].
     * @throws NetworkError when the request fails.
     */
    suspend fun execute(
        request: Request,
        classifier: RetryClassifier = RetryClassifier.AcceptAll,
    ): NetworkResponse {
        require(request.url.isHttps) { "HTTPS only: ${request.url}" }
        if (request.method != "GET") return fetchWithRetry(request, classifier)
        val key = request.url.toString()
        val shared =
            synchronized(inFlight) {
                inFlight.getOrPut(key) {
                    scope.async(start = CoroutineStart.LAZY) { fetchWithRetry(request, classifier) }.also { call ->
                        call.invokeOnCompletion { synchronized(inFlight) { inFlight.remove(key, call) } }
                    }
                }
            }
        shared.start()
        return shared.await()
    }

    private suspend fun fetchWithRetry(
        request: Request,
        classifier: RetryClassifier,
    ): NetworkResponse {
        for (backoff in BACKOFF) {
            when (val outcome = attempt(request, classifier)) {
                is Outcome.Done -> return outcome.response
                is Outcome.Retry -> delay(backoff)
            }
        }
        return when (val outcome = attempt(request, classifier)) {
            is Outcome.Done -> outcome.response
            is Outcome.Retry -> throw outcome.finalError
        }
    }

    private suspend fun attempt(
        request: Request,
        classifier: RetryClassifier,
    ): Outcome {
        if (request.url.encodedPath.startsWith(API_PATH)) awaitApiSlot()
        val response =
            try {
                transport.execute(request)
            } catch (e: IOException) {
                throw NetworkError.Offline(e)
            }
        return when {
            response.code == HTTP_TOO_MANY_REQUESTS -> Outcome.Retry(NetworkError.RateLimited())
            response.code in UNAVAILABLE_CODES -> Outcome.Retry(NetworkError.Unavailable(response.code))
            !response.isSuccessful && response.code !in CLIENT_ERROR_CODES -> Outcome.Done(response)
            else ->
                when (val decision = classifier.classify(response)) {
                    RetryDecision.Accept -> Outcome.Done(response)
                    is RetryDecision.Retry -> Outcome.Retry(decision.finalError)
                    is RetryDecision.Fail -> throw decision.error
                }
        }
    }

    /** Waits for the API slot and holds it for [API_INTERVAL] from now, whatever happens to the caller. */
    private suspend fun awaitApiSlot() {
        apiSlot.lock()
        scope.launch {
            delay(API_INTERVAL)
            apiSlot.unlock()
        }
    }

    private sealed interface Outcome {
        class Done(
            val response: NetworkResponse,
        ) : Outcome

        class Retry(
            val finalError: NetworkError,
        ) : Outcome
    }

    companion object {
        /** Codeforces allows one API call per 2 s. */
        val API_INTERVAL: Duration = 2.seconds

        /** Waits before each retry; after the last one the call fails. */
        val BACKOFF: List<Duration> = listOf(2.seconds, 4.seconds, 8.seconds)

        private const val API_PATH = "/api/"
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val UNAVAILABLE_CODES = setOf(502, 503, 504)
        private val CLIENT_ERROR_CODES = 400..499
    }
}
