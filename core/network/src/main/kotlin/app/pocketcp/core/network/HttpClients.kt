package app.pocketcp.core.network

import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

/** Builds the app's single [OkHttpClient]. */
object HttpClients {
    /** Disk size of the HTTP cache that serves ETag / If-Modified-Since revalidations. */
    const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L

    /**
     * HTTP/2 with HTTP/1.1 fallback, transparent gzip (OkHttp's default), conditional requests through [cache],
     * and HTTPS only: without `ConnectionSpec.CLEARTEXT`, `http://` calls fail before any byte is sent.
     */
    fun create(
        cache: Cache,
        cookieJar: CookieJar,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .cache(cache)
            .cookieJar(cookieJar)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
}
