package app.pocketcp.core.codeforces

import app.pocketcp.core.network.NetworkError
import app.pocketcp.core.network.NetworkResponse
import app.pocketcp.core.network.RetryClassifier
import app.pocketcp.core.network.RetryDecision

/**
 * Makes the [app.pocketcp.core.network.RequestQueue] retry a "Call limit exceeded" envelope with its backoff,
 * whether it comes with HTTP 200 or, as real Codeforces sends it, HTTP 400.
 * Once retries run out the queue throws [NetworkError.Rejected], which [CodeforcesApi] reports as
 * [CodeforcesError.RateLimited]. Every other body is accepted and left to the envelope decoding.
 */
internal object CallLimitClassifier : RetryClassifier {
    override fun classify(response: NetworkResponse): RetryDecision {
        // Fast path: result bodies can be megabytes, so only decode the header when the comment can be there.
        if (CALL_LIMIT_COMMENT !in response.body) return RetryDecision.Accept
        val header = EnvelopeHeader.parse(response.body)
        return if (header?.isCallLimit == true) {
            RetryDecision.Retry(NetworkError.Rejected(CALL_LIMIT_COMMENT))
        } else {
            RetryDecision.Accept
        }
    }
}
