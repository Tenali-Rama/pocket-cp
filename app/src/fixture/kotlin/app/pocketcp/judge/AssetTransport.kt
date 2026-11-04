package app.pocketcp.judge

import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.NetworkResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.FileNotFoundException

/** Reads one recorded Codeforces response by file name; throws [FileNotFoundException] when there is none. */
fun interface FixtureAssets {
    fun read(name: String): String
}

/**
 * An [HttpTransport] that answers Codeforces calls from [assets] instead of the network. API calls use
 * `<method>.json`, or `<method>.gym.json` for `gym=true`; problem pages use `contest/<id>/problem/<index>.<lang>.html`.
 * A missing file is HTTP 404, which the client reports as malformed. Files are read on [dispatcher].
 */
internal class AssetTransport(
    private val assets: FixtureAssets,
    private val dispatcher: CoroutineDispatcher,
) : HttpTransport {
    override suspend fun execute(request: Request): NetworkResponse {
        val name = fixtureName(request.url)
        val body =
            withContext(dispatcher) {
                try {
                    assets.read(name)
                } catch (ignored: FileNotFoundException) {
                    null
                }
            }
        return if (body == null) {
            NetworkResponse(HTTP_NOT_FOUND, headersOf(), "")
        } else if (isPage(request.url)) {
            NetworkResponse(HTTP_OK, HTML_HEADERS, body)
        } else {
            NetworkResponse(HTTP_OK, JSON_HEADERS, body)
        }
    }

    private fun fixtureName(url: HttpUrl): String {
        if (isPage(url)) {
            val language = url.queryParameter("locale") ?: "en"
            return "${url.pathSegments.joinToString("/")}.$language.html"
        }
        val variant = if (url.queryParameter("gym") == "true") ".gym" else ""
        return url.pathSegments.last() + variant + ".json"
    }

    private fun isPage(url: HttpUrl): Boolean = url.pathSegments.firstOrNull() in PAGE_ROOTS

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
        val HTML_HEADERS = headersOf("Content-Type", "text/html; charset=utf-8")
        val PAGE_ROOTS = setOf("contest", "gym")
    }
}
