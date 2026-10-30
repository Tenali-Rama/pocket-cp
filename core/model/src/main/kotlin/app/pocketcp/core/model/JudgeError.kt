package app.pocketcp.core.model

/** The only failure a [JudgeSource] call throws. Each case maps to one user-visible state. */
sealed class JudgeError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** No connection or a timeout. */
    class Offline(
        cause: Throwable? = null,
    ) : JudgeError("Network unreachable", cause)

    /** The judge's rate limit is still hit after the queue's retries. */
    class RateLimited(
        cause: Throwable? = null,
    ) : JudgeError("Judge rate limit exceeded", cause)

    /** The judge is down or temporarily unreachable (e.g. HTTP 503); other unexpected statuses are `Malformed`. */
    class Unavailable(
        cause: Throwable? = null,
    ) : JudgeError("Judge unavailable", cause)

    /** The judge refused the request with a reason to show, e.g. "User with handle x not found". */
    class Rejected(
        message: String,
        cause: Throwable? = null,
    ) : JudgeError(message, cause)

    /** A response that doesn't match the expected shape. */
    class Malformed(
        cause: Throwable? = null,
    ) : JudgeError("Malformed judge response", cause)
}
