package app.pocketcp.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/** Performs one HTTP exchange. Throws [IOException] when the network fails. */
fun interface HttpTransport {
    suspend fun execute(request: Request): NetworkResponse
}

/** [HttpTransport] over OkHttp. Reads the body on OkHttp's threads; cancelling the coroutine cancels the call. */
class OkHttpTransport(
    private val callFactory: Call.Factory,
) : HttpTransport {
    override suspend fun execute(request: Request): NetworkResponse {
        val call = callFactory.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        continuation.resumeWith(runCatching { response.use { it.toNetworkResponse() } })
                    }
                },
            )
        }
    }

    private fun Response.toNetworkResponse(): NetworkResponse {
        val text = body?.string().orEmpty()
        return NetworkResponse(code = code, headers = headers, body = text)
    }
}
