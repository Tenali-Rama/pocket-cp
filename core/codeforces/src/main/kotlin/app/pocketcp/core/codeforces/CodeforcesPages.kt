package app.pocketcp.core.codeforces

import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.network.NetworkError
import app.pocketcp.core.network.RequestQueue
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/** Fetches Codeforces HTML problem pages through the shared [RequestQueue]. */
class CodeforcesPages(
    private val queue: RequestQueue,
    private val baseUrl: HttpUrl = BASE_URL,
) {
    /** Fetches the problem page for [problem] in [language]. */
    suspend fun problem(
        problem: ProblemId,
        language: StatementLanguage,
    ): String {
        val response =
            try {
                queue.execute(Request.Builder().url(problemUrl(problem, language)).build())
            } catch (error: NetworkError) {
                throw error.toCodeforcesError()
            }
        if (!response.isSuccessful) throw CodeforcesError.Http(response.code)
        return response.body
    }

    /** Builds the URL used by [problem], without performing network I/O. */
    fun problemUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String =
        baseUrl
            .newBuilder()
            .addPathSegment(if (problem.contestId >= GYM_CONTEST_ID) "gym" else "contest")
            .addPathSegment(problem.contestId.toString())
            .addPathSegment("problem")
            .addPathSegment(problem.index)
            .addQueryParameter("locale", language.key)
            .build()
            .toString()

    private fun NetworkError.toCodeforcesError(): CodeforcesError =
        when (this) {
            is NetworkError.RateLimited -> CodeforcesError.RateLimited()
            is NetworkError.Unavailable -> CodeforcesError.Unavailable(code)
            is NetworkError.Offline -> CodeforcesError.Offline(cause)
            is NetworkError.Rejected -> CodeforcesError.Api(reason)
        }

    companion object {
        /** The public Codeforces page host, without an API path. */
        val BASE_URL: HttpUrl = "https://codeforces.com/".toHttpUrl()
        private const val GYM_CONTEST_ID = 100_000
    }
}
