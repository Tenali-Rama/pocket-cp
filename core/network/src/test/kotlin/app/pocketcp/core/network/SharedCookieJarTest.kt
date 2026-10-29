package app.pocketcp.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedCookieJarTest {
    private var time = System.currentTimeMillis()
    private val jar = SharedCookieJar(InMemoryCookieStore()) { time }

    @Test
    fun `a codeforces cookie is sent back to codeforces and never to another host`() {
        setCookie(CODEFORCES, "JSESSIONID=abc; Path=/; Secure; HttpOnly")

        assertEquals(listOf("JSESSIONID=abc"), sent("https://codeforces.com/problemset/problem/1/A"))
        assertTrue(sent("https://example.com/").isEmpty())
        assertTrue(sent("https://notcodeforces.com/").isEmpty())
        assertTrue(sent("https://codeforces.com.evil.io/").isEmpty())
    }

    @Test
    fun `a host-only cookie is not sent to subdomains, a domain cookie is`() {
        setCookie(CODEFORCES, "host=1; Path=/")
        setCookie(CODEFORCES, "dom=2; Domain=codeforces.com; Path=/")

        assertEquals(listOf("dom=2"), sent("https://m1.codeforces.com/"))
    }

    @Test
    fun `a newer cookie replaces the stored one`() {
        setCookie(CODEFORCES, "JSESSIONID=old; Path=/")
        setCookie(CODEFORCES, "JSESSIONID=new; Path=/")

        assertEquals(listOf("JSESSIONID=new"), sent("https://codeforces.com/"))
    }

    @Test
    fun `an expired cookie is no longer sent`() {
        setCookie(CODEFORCES, "token=t; Path=/; Max-Age=60")
        assertEquals(listOf("token=t"), sent("https://codeforces.com/"))

        time += 61_000
        assertTrue(sent("https://codeforces.com/").isEmpty())
    }

    private fun setCookie(
        url: HttpUrl,
        header: String,
    ) {
        val cookie = requireNotNull(Cookie.parse(url, header)) { "unparseable cookie: $header" }
        jar.saveFromResponse(url, listOf(cookie))
    }

    private fun sent(url: String) = jar.loadForRequest(url.toHttpUrl()).map { "${it.name}=${it.value}" }

    private companion object {
        val CODEFORCES = "https://codeforces.com/".toHttpUrl()
    }
}
