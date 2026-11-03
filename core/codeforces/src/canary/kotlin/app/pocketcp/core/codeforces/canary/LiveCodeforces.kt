package app.pocketcp.core.codeforces.canary

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesError
import app.pocketcp.core.codeforces.CodeforcesPages
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.network.HttpClients
import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.InMemoryCookieStore
import app.pocketcp.core.network.NetworkError
import app.pocketcp.core.network.NetworkResponse
import app.pocketcp.core.network.OkHttpTransport
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.network.SharedCookieJar
import app.pocketcp.core.parser.CodeforcesStatementParser
import app.pocketcp.core.parser.StatementParseError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.Cache
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The app's network stack against live Codeforces: the production client config, one [RequestQueue] and a
 * [CodeforcesApi] on it. The transport under the queue keeps each path's last raw response, so a failure the
 * queue or the API reports only as a status can still be told apart from a Cloudflare challenge.
 */
class LiveCodeforces(
    cacheDir: File,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client =
        HttpClients.create(Cache(cacheDir, HttpClients.CACHE_SIZE_BYTES), SharedCookieJar(InMemoryCookieStore()))
    private val transport = RecordingTransport(OkHttpTransport(client))
    private val queue = RequestQueue(transport, scope)
    private val api = CodeforcesApi(queue, scope)
    private val pages = CodeforcesPages(queue)

    /** Runs [call] on the API; a failure throws an [AssertionError] naming [endpoint] or the Cloudflare block. */
    suspend fun <T> endpoint(
        endpoint: String,
        call: suspend (CodeforcesApi) -> T,
    ): T =
        try {
            call(api)
        } catch (error: CodeforcesError) {
            throw failure(API_PATH + endpoint, error) { CanaryReport.endpointBroken(endpoint, it) }
        }

    /** Fetches [url] through the queue and fails unless it contains [selector]. */
    suspend fun page(
        url: String,
        selector: String,
    ) {
        val response = fetchPage(url)
        CanaryReport.checkPage(url, response, selector)?.let { throw AssertionError(it) }
    }

    /** Fetches and parses one statement, reporting parser failures in the canary's issue-friendly format. */
    suspend fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Statement {
        val url = pages.problemUrl(problem, language)
        val html = page(url)
        return try {
            CodeforcesStatementParser.parse(html, problem, language)
        } catch (error: StatementParseError.Blocked) {
            throw AssertionError(CanaryReport.blocked(url), error)
        } catch (error: StatementParseError.UnknownMarkup) {
            throw AssertionError(CanaryReport.pageBroken(url, error.message.orEmpty()), error)
        }
    }

    /** Fetches one successful page body for the parser canary. */
    private suspend fun page(url: String): String {
        val response = fetchPage(url)
        if (!response.isSuccessful) {
            throw AssertionError(CanaryReport.checkPage(url, response, STATEMENT_SELECTOR))
        }
        return response.body
    }

    private suspend fun fetchPage(url: String): NetworkResponse {
        val request = Request.Builder().url(url).build()
        return try {
            queue.execute(request)
        } catch (error: NetworkError) {
            throw failure(request.url.encodedPath, error) { CanaryReport.pageBroken(url, it) }
        }
    }

    /** [error] on [path] as a check failure: a Cloudflare block when the last response there was a challenge. */
    private fun failure(
        path: String,
        error: Exception,
        describe: (String) -> String,
    ): AssertionError {
        val last = transport.last(path)
        val message =
            if (last != null && CanaryReport.isCloudflareChallenge(last.response)) {
                CanaryReport.blocked(last.url)
            } else {
                describe(error.message.orEmpty())
            }
        return AssertionError(message, error)
    }

    override fun close() {
        scope.cancel()
        client.cache?.close()
    }

    private companion object {
        const val API_PATH = "/api/"
        const val STATEMENT_SELECTOR = "div.problem-statement"
    }
}

/** An [HttpTransport] that remembers the last response per URL path. */
private class RecordingTransport(
    private val delegate: HttpTransport,
) : HttpTransport {
    class Exchange(
        val url: String,
        val response: NetworkResponse,
    )

    private val exchanges = ConcurrentHashMap<String, Exchange>()

    override suspend fun execute(request: Request): NetworkResponse =
        delegate.execute(request).also { exchanges[request.url.encodedPath] = Exchange(request.url.toString(), it) }

    fun last(path: String): Exchange? = exchanges[path]
}
