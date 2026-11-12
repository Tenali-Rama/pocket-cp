package app.pocketcp.core.webview

import java.net.URI

/** Which URLs the in-app WebView may load (spec §16, ADR 0021): `https` on `codeforces.com` or a subdomain of it. */
object CodeforcesUrlPolicy {
    private const val DOMAIN = "codeforces.com"

    /** Whether [url] is `https` and its host is `codeforces.com` or ends in `.codeforces.com`. */
    fun isAllowed(url: String): Boolean {
        // Not parsable as a URI: not a Codeforces URL either.
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && (host == DOMAIN || host.endsWith(".$DOMAIN"))
    }
}
