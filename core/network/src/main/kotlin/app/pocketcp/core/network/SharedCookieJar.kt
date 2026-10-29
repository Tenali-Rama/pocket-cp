package app.pocketcp.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Where [SharedCookieJar] keeps its cookies. In memory for now; M4 backs it with encrypted storage. */
interface CookieStore {
    fun all(): List<Cookie>

    /** Adds [cookies], replacing any stored cookie with the same name, domain and path. */
    fun put(cookies: List<Cookie>)
}

class InMemoryCookieStore : CookieStore {
    private val cookies = mutableListOf<Cookie>()

    override fun all(): List<Cookie> = synchronized(cookies) { cookies.toList() }

    override fun put(cookies: List<Cookie>) {
        synchronized(this.cookies) {
            this.cookies.removeAll { old -> cookies.any { it.sameIdentity(old) } }
            this.cookies += cookies
        }
    }

    private fun Cookie.sameIdentity(other: Cookie) = name == other.name && domain == other.domain && path == other.path
}

/**
 * The one cookie jar of the app (spec §4), shared by every request and, from M4, the WebView.
 * A cookie is only sent where its domain, path and `Secure` flag allow, and never once it has expired.
 */
class SharedCookieJar(
    private val store: CookieStore,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) = store.put(cookies)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val time = now()
        return store.all().filter { it.expiresAt > time && it.matches(url) }
    }
}
