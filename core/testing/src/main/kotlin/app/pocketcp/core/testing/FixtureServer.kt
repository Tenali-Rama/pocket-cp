package app.pocketcp.core.testing

import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.NetworkResponse
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An [HttpTransport] that answers from recorded fixtures instead of the network, for a `RequestQueue` under test.
 * Requests are matched on path plus decoded query, e.g. `/api/user.info?handles=tourist;jiangly`.
 * An unmapped request fails the test with an [AssertionError] naming its path.
 */
class FixtureServer : HttpTransport {
    private val routes = ConcurrentHashMap<String, Route>()
    private val received = CopyOnWriteArrayList<Request>()

    /** Every request served so far, in order. */
    val requests: List<Request> get() = received.toList()

    /** Answers [path] (path plus query, as matched) with `testdata/<fixture>` and HTTP [code]. */
    fun map(
        path: String,
        fixture: String,
        code: Int = HTTP_OK,
    ): FixtureServer = apply { routes[path] = Route(fixture, code) }

    override suspend fun execute(request: Request): NetworkResponse {
        received += request
        val path = request.url.pathAndQuery()
        val route = routes[path] ?: throw AssertionError("No fixture mapped for $path (mapped: ${routes.keys})")
        val headers = headersOf(CONTENT_TYPE, contentType(route.fixture))
        return NetworkResponse(route.code, headers, Fixtures.text(route.fixture))
    }

    private data class Route(
        val fixture: String,
        val code: Int,
    )

    companion object {
        const val HTTP_OK = 200
        private const val CONTENT_TYPE = "Content-Type"
        private const val JSON = "application/json"
        private const val HTML = "text/html; charset=utf-8"

        /** The key a request is matched on: its path and, if any, `?` and its decoded query. */
        private fun HttpUrl.pathAndQuery(): String = encodedPath + (query?.let { "?$it" } ?: "")

        private fun contentType(fixture: String): String = if (fixture.endsWith(".json")) JSON else HTML
    }
}
