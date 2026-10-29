package app.pocketcp.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownServiceException

class HttpClientTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        val host = InetAddress.getByName("localhost").canonicalHostName
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName(host).build()
        server.useHttps(
            HandshakeCertificates
                .Builder()
                .heldCertificate(certificate)
                .build()
                .sslSocketFactory(),
            false,
        )
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        client =
            HttpClients
                .create(Cache(tmp.newFolder("http"), HttpClients.CACHE_SIZE_BYTES), SharedCookieJar(InMemoryCookieStore()))
                .newBuilder()
                .sslSocketFactory(trust.sslSocketFactory(), trust.trustManager)
                .build()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `a 304 to If-None-Match is served from the cache`() {
        server.enqueue(MockResponse().setBody("contests").setHeader("ETag", "\"v1\"").setHeader("Cache-Control", "no-cache"))
        server.enqueue(MockResponse().setResponseCode(304))

        call("/api/contest.list").close()
        val second = call("/api/contest.list")

        server.takeRequest()
        assertEquals("\"v1\"", server.takeRequest().getHeader("If-None-Match"))
        assertEquals(200, second.code)
        assertEquals(304, second.networkResponse?.code)
        assertNotNull(second.cacheResponse)
        assertEquals("contests", second.body?.string())
    }

    @Test
    fun `plain http is rejected before reaching the server`() {
        val plain = MockWebServer()
        try {
            val result = runCatching { client.newCall(Request.Builder().url(plain.url("/")).build()).execute() }
            assertTrue("got $result", result.exceptionOrNull() is UnknownServiceException)
            assertEquals(0, plain.requestCount)
        } finally {
            plain.shutdown()
        }
    }

    @Test
    fun `cookies set by a response are sent on the next request`() {
        server.enqueue(MockResponse().setHeader("Set-Cookie", "JSESSIONID=abc; Path=/"))
        server.enqueue(MockResponse())

        call("/enter").close()
        call("/profile").close()

        assertNull(server.takeRequest().getHeader("Cookie"))
        assertEquals("JSESSIONID=abc", server.takeRequest().getHeader("Cookie"))
    }

    @Test
    fun `the transport reads status and body`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))

        val response = runBlocking { OkHttpTransport(client).execute(request("/api/user.info")) }

        assertEquals(404, response.code)
        assertEquals("missing", response.body)
    }

    @Test
    fun `the transport surfaces a dropped connection as an IOException`() {
        val request = request("/api/user.info")
        server.shutdown()

        val result = runCatching { runBlocking { OkHttpTransport(client).execute(request) } }

        assertTrue("got $result", result.exceptionOrNull() is IOException)
    }

    private fun request(path: String) = Request.Builder().url(server.url(path)).build()

    private fun call(path: String) = client.newCall(request(path)).execute()
}
