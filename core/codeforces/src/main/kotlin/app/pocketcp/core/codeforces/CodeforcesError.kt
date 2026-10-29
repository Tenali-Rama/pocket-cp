package app.pocketcp.core.codeforces

/** Typed failure of a [CodeforcesApi] call. Callers map each case to a user-visible state. */
sealed class CodeforcesError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** `status: FAILED` with the API's [comment], e.g. "handles: User with handle x not found". */
    class Api(
        val comment: String,
    ) : CodeforcesError(comment)

    /** "Call limit exceeded" or HTTP 429, still there after the queue's retries. */
    class RateLimited : CodeforcesError("Codeforces call limit exceeded")

    /** HTTP 502/503/504 after the queue's retries: Codeforces is down, which is not rate limiting. */
    class Unavailable(
        val code: Int,
    ) : CodeforcesError("Codeforces unavailable (HTTP $code)")

    /** No connection or a timeout. */
    class Offline(
        cause: Throwable?,
    ) : CodeforcesError("Network unreachable", cause)

    /** Any other HTTP status whose body is not an API envelope (e.g. an HTML error page). */
    class Http(
        val code: Int,
    ) : CodeforcesError("Unexpected HTTP $code from Codeforces")

    /** A body that doesn't match the expected shape. */
    class Malformed(
        detail: String,
        cause: Throwable? = null,
    ) : CodeforcesError("Malformed Codeforces response: $detail", cause)
}
