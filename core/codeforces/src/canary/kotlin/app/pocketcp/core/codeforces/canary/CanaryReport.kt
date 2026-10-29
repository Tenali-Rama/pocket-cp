package app.pocketcp.core.codeforces.canary

import app.pocketcp.core.network.NetworkResponse
import org.jsoup.Jsoup

/**
 * The one-line messages a failing canary check prints. canary.yml puts the last 40 lines of output in an issue,
 * so each message names what broke: the endpoint, or the selector and its page, or a Cloudflare block.
 */
object CanaryReport {
    private val CHALLENGE_CODES = setOf(403, 503)
    private const val MITIGATED_HEADER = "cf-mitigated"

    /** Markers of Cloudflare's challenge and block pages. */
    private val CHALLENGE_MARKERS =
        listOf("challenge-platform", "cf-chl-", "<title>Just a moment...</title>", "Attention Required! | Cloudflare")

    fun blocked(url: String): String = "BLOCKED by Cloudflare: $url"

    fun selectorBroken(
        selector: String,
        url: String,
    ): String = "SELECTOR BROKEN: $selector on $url"

    fun endpointBroken(
        endpoint: String,
        detail: String,
    ): String = "ENDPOINT BROKEN: $endpoint: $detail"

    fun pageBroken(
        url: String,
        detail: String,
    ): String = "PAGE BROKEN: $url: $detail"

    /** A 403/503 that Cloudflare marked (`cf-mitigated`) or whose body is a challenge page. */
    fun isCloudflareChallenge(response: NetworkResponse): Boolean =
        response.code in CHALLENGE_CODES &&
            (response.headers[MITIGATED_HEADER] != null || CHALLENGE_MARKERS.any { it in response.body })

    /** Why the page at [url] doesn't contain [selector], or null when it does. */
    fun checkPage(
        url: String,
        response: NetworkResponse,
        selector: String,
    ): String? =
        when {
            isCloudflareChallenge(response) -> blocked(url)
            !response.isSuccessful -> pageBroken(url, "HTTP ${response.code}")
            Jsoup.parse(response.body, url).selectFirst(selector) == null -> selectorBroken(selector, url)
            else -> null
        }
}
