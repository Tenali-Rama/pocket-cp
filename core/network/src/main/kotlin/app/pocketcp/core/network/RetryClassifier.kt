package app.pocketcp.core.network

/** Decides whether a 2xx [NetworkResponse] is usable, so a caller can treat a body as an error (e.g. "Call limit exceeded"). */
fun interface RetryClassifier {
    fun classify(response: NetworkResponse): RetryDecision

    companion object {
        /** Accepts every 2xx response. The default. */
        val AcceptAll = RetryClassifier { RetryDecision.Accept }
    }
}

sealed interface RetryDecision {
    /** Return the response to the caller. */
    data object Accept : RetryDecision

    /** Retry with the queue's backoff; once retries run out, fail with [finalError]. */
    data class Retry(
        val finalError: NetworkError,
    ) : RetryDecision

    /** Fail at once with [error]. */
    data class Fail(
        val error: NetworkError,
    ) : RetryDecision
}
