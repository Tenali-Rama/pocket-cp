package app.pocketcp.core.network

import java.io.IOException

/** Typed failure of a [RequestQueue] call. Callers map each case to a user-visible state. */
sealed class NetworkError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** HTTP 429 after every retry. */
    class RateLimited : NetworkError("Rate limited by the server")

    /** HTTP 502/503/504 after every retry: the server is down, which is not the same as rate limiting. */
    class Unavailable(
        val code: Int,
    ) : NetworkError("Server unavailable (HTTP $code)")

    /** No connection or a timeout. Never retried. */
    class Offline(
        cause: IOException,
    ) : NetworkError("Network unreachable", cause)

    /** A 2xx response that a [RetryClassifier] refused, with its reason (e.g. the API's comment). */
    class Rejected(
        val reason: String,
    ) : NetworkError(reason)
}
