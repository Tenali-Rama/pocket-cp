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
 * An [HttpTransport] that answers Codeforces API calls from [assets] instead of the network: `<method>.json`,
 * or `<method>.gym.json` for `gym=true` (the `testdata/codeforces/api/` names). Other parameters are ignored.
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
        } else {
            NetworkResponse(HTTP_OK, JSON_HEADERS, body)
        }
    }

    private fun fixtureName(url: HttpUrl): String {
        val variant = if (url.queryParameter("gym") == "true") ".gym" else ""
        return url.pathSegments.last() + variant + ".json"
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
    }
}
