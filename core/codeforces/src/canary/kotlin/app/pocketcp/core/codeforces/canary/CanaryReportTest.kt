package app.pocketcp.core.codeforces.canary

import app.pocketcp.core.network.NetworkResponse
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** The canary's failure messages on small inline samples (no network). */
class CanaryReportTest {
    @Test
    fun `a page with the selector passes`() {
        val page = response(200, "<html><body><div class=\"problem-statement\">Watermelon</div></body></html>")

        assertNull(CanaryReport.checkPage(URL, page, SELECTOR))
    }

    @Test
    fun `a page without the selector names the selector and the page`() {
        val page = response(200, "<html><body><div class=\"statement\">Watermelon</div></body></html>")

        assertEquals("SELECTOR BROKEN: div.problem-statement on $URL", CanaryReport.checkPage(URL, page, SELECTOR))
    }

    @Test
    fun `a 403 marked cf-mitigated is a Cloudflare block`() {
        val challenge = response(403, "<html></html>", "cf-mitigated", "challenge")

        assertEquals("BLOCKED by Cloudflare: $URL", CanaryReport.checkPage(URL, challenge, SELECTOR))
    }

    @Test
    fun `a 503 challenge page is a Cloudflare block`() {
        val body =
            "<html><head><title>Just a moment...</title></head><body><div id=\"cf-chl-widget\"></div></body></html>"

        assertEquals("BLOCKED by Cloudflare: $URL", CanaryReport.checkPage(URL, response(503, body), SELECTOR))
    }

    @Test
    fun `a plain 403 is not a Cloudflare block`() {
        val forbidden = response(403, "<html><body>Forbidden</body></html>")

        assertFalse(CanaryReport.isCloudflareChallenge(forbidden))
        assertEquals("PAGE BROKEN: $URL: HTTP 403", CanaryReport.checkPage(URL, forbidden, SELECTOR))
    }

    @Test
    fun `a challenge body with 200 is not a block, so the selector check reports it`() {
        val page = response(200, "<html><head><title>Just a moment...</title></head></html>")

        assertEquals("SELECTOR BROKEN: div.problem-statement on $URL", CanaryReport.checkPage(URL, page, SELECTOR))
    }

    private fun response(
        code: Int,
        body: String,
        vararg headers: String,
    ) = NetworkResponse(code, headersOf(*headers), body)

    private companion object {
        const val URL = "https://codeforces.com/problemset/problem/4/A"
        const val SELECTOR = "div.problem-statement"
    }
}
