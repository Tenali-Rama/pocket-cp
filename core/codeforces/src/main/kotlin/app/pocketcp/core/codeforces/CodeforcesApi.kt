package app.pocketcp.core.codeforces

import app.pocketcp.core.network.NetworkError
import app.pocketcp.core.network.RequestQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Typed client for the public Codeforces API. Every call goes through [queue] (1 call / 2 s, dedup, backoff),
 * and "Call limit exceeded" is retried there with the queue's backoff.
 * Each method returns the envelope's `result` or throws [CodeforcesError].
 * @param scope where queued calls run; it should live as long as the app (calls survive a cancelled caller).
 */
class CodeforcesApi(
    queue: RequestQueue,
    scope: CoroutineScope,
    baseUrl: HttpUrl = BASE_URL,
) {
    private val service: CodeforcesService =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .callFactory(QueueCallFactory(queue, CallLimitClassifier, scope))
            .addConverterFactory(CodeforcesJson.asConverterFactory(JSON))
            .build()
            .create(CodeforcesService::class.java)

    /** `contest.list`: regular contests, or Gym contests when [gym] is true. */
    suspend fun contests(gym: Boolean = false): List<ContestDto> = call { service.contestList(gym.takeIf { it }) }

    /** `problemset.problems`: problems having all of [tags] (all problems when empty), with their solve counts. */
    suspend fun problemset(tags: List<String> = emptyList()): ProblemsetDto = call { service.problemsetProblems(tags.joinOrNull()) }

    /** `user.info`: one profile per handle, in the order of [handles]. */
    suspend fun users(handles: List<String>): List<UserDto> {
        require(handles.isNotEmpty()) { "At least one handle" }
        return call { service.userInfo(handles.joinToString(SEPARATOR)) }
    }

    private suspend fun <T> call(request: suspend () -> Envelope<T>): T {
        val envelope = runCatching { request() }.getOrElse { throw it.toCodeforcesError() }
        return envelope.resultOrThrow()
    }

    /**
     * Queue, HTTP and decoding failures become [CodeforcesError]s. Anything else (cancellation, a bug,
     * a failed test assertion) is returned as it is, to be rethrown.
     */
    private fun Throwable.toCodeforcesError(): Throwable =
        when (this) {
            is QueueCallFailure -> cause.toCodeforcesError()
            is NetworkError.RateLimited -> CodeforcesError.RateLimited()
            is NetworkError.Rejected -> failure(reason)
            is NetworkError.Unavailable -> CodeforcesError.Unavailable(code)
            is NetworkError.Offline -> CodeforcesError.Offline(cause)
            is HttpException -> envelopeError()
            is SerializationException -> CodeforcesError.Malformed(message.orEmpty(), this)
            else -> this
        }

    /** Codeforces answers `FAILED` with HTTP 400; other statuses without an envelope are unexpected. */
    private fun HttpException.envelopeError(): CodeforcesError {
        val header = response()?.errorBody()?.string()?.let(EnvelopeHeader::parse)
        return if (header?.isFailed == true) failure(header.comment) else CodeforcesError.Http(code())
    }

    private fun List<String>.joinOrNull(): String? = if (isEmpty()) null else joinToString(SEPARATOR)

    companion object {
        val BASE_URL: HttpUrl = "https://codeforces.com/api/".toHttpUrl()
        private const val SEPARATOR = ";"
        private val JSON = "application/json".toMediaType()
    }
}
