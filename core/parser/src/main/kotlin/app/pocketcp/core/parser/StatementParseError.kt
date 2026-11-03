package app.pocketcp.core.parser

/** The only failures exposed by the Codeforces statement parser. */
sealed class StatementParseError(
    message: String,
) : Exception(message) {
    /** The response is a Cloudflare challenge page. */
    data object Blocked : StatementParseError("Codeforces returned a Cloudflare challenge")

    /** The response does not match the supported Codeforces statement markup. */
    data class UnknownMarkup(
        val detail: String,
    ) : StatementParseError(detail)
}
