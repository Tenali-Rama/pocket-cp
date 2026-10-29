package app.pocketcp.core.codeforces

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The JSON settings for every Codeforces body: fields the client doesn't use are skipped. */
internal val CodeforcesJson = Json { ignoreUnknownKeys = true }

/** Every API response: `{"status":"OK","result":…}` or `{"status":"FAILED","comment":…}`. */
@Serializable
internal data class Envelope<T>(
    val status: String,
    val comment: String? = null,
    val result: T? = null,
) {
    /** The result, or the [CodeforcesError] this envelope stands for. */
    fun resultOrThrow(): T {
        if (status == STATUS_OK) return result ?: throw CodeforcesError.Malformed("OK response without a result")
        throw failure(comment)
    }
}

/** Just the status and comment, for bodies that are not decoded into a result (errors, the retry check). */
@Serializable
internal data class EnvelopeHeader(
    val status: String,
    val comment: String? = null,
) {
    val isFailed: Boolean get() = status == STATUS_FAILED

    val isCallLimit: Boolean get() = isFailed && isCallLimitComment(comment)

    companion object {
        /** Decodes [body], or returns null when it isn't an API envelope (e.g. an HTML error page). */
        fun parse(body: String): EnvelopeHeader? =
            try {
                CodeforcesJson.decodeFromString(serializer(), body)
            } catch (ignored: SerializationException) {
                null
            }
    }
}

/** Codeforces' comment when a client exceeds 1 call / 2 s. */
internal const val CALL_LIMIT_COMMENT = "Call limit exceeded"

internal fun isCallLimitComment(comment: String?): Boolean = comment?.startsWith(CALL_LIMIT_COMMENT) == true

/** The error for a `FAILED` envelope with [comment]. */
internal fun failure(comment: String?): CodeforcesError =
    if (isCallLimitComment(comment)) CodeforcesError.RateLimited() else CodeforcesError.Api(comment.orEmpty())

private const val STATUS_OK = "OK"
private const val STATUS_FAILED = "FAILED"
