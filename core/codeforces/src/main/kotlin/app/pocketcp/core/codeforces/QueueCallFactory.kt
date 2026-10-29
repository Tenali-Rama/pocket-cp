package app.pocketcp.core.codeforces

import app.pocketcp.core.network.NetworkResponse
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.network.RetryClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retrofit's `callFactory`: every call Retrofit makes runs through [queue] with [classifier], so the
 * service gets the queue's spacing, dedup and backoff instead of calling OkHttp itself.
 * Only asynchronous calls exist (Retrofit's `suspend` methods use [Call.enqueue]); they run in [scope].
 */
internal class QueueCallFactory(
    private val queue: RequestQueue,
    private val classifier: RetryClassifier,
    private val scope: CoroutineScope,
) : Call.Factory {
    override fun newCall(request: Request): Call = QueueCall(request)

    private inner class QueueCall(
        private val request: Request,
    ) : Call {
        private val executed = AtomicBoolean()
        private val canceled = AtomicBoolean()

        @Volatile private var job: Job? = null

        override fun request(): Request = request

        override fun execute(): Response = throw UnsupportedOperationException("Queued calls are asynchronous only")

        override fun enqueue(responseCallback: Callback) {
            check(executed.compareAndSet(false, true)) { "Already executed" }
            job =
                scope.launch {
                    val outcome = runCatching { queue.execute(request, classifier) }
                    outcome.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                    outcome.fold(
                        onSuccess = { responseCallback.onResponse(this@QueueCall, it.toResponse()) },
                        onFailure = { responseCallback.onFailure(this@QueueCall, QueueCallFailure(it)) },
                    )
                }
            if (canceled.get()) job?.cancel()
        }

        override fun cancel() {
            canceled.set(true)
            job?.cancel()
        }

        override fun isExecuted(): Boolean = executed.get()

        override fun isCanceled(): Boolean = canceled.get()

        override fun timeout(): Timeout = Timeout.NONE

        override fun clone(): Call = QueueCall(request)

        private fun NetworkResponse.toResponse(): Response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .headers(headers)
                .body(body.toResponseBody(headers[CONTENT_TYPE]?.toMediaTypeOrNull()))
                .build()
    }

    private companion object {
        const val CONTENT_TYPE = "Content-Type"
    }
}

/**
 * Carries a queue failure through Retrofit, whose callback only takes [IOException]s.
 * [CodeforcesApi] unwraps it into a [CodeforcesError] (or rethrows [cause] as it is).
 */
internal class QueueCallFailure(
    override val cause: Throwable,
) : IOException(cause.message, cause)
