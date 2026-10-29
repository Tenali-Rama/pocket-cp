package app.pocketcp.core.network

import okhttp3.Headers

/** A fully read HTTP response, so one result can be shared by every deduplicated caller. */
data class NetworkResponse(
    val code: Int,
    val headers: Headers,
    val body: String,
) {
    val isSuccessful: Boolean get() = code in SUCCESS_CODES

    private companion object {
        val SUCCESS_CODES = 200..299
    }
}
