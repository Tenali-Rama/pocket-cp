package app.pocketcp.core.network

import android.content.Context
import android.net.ConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

/** One client, one cookie jar, one request queue and one network monitor for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CACHE_DIR = "http"

    @Provides
    @Singleton
    fun provideCookieStore(): CookieStore = InMemoryCookieStore()

    @Provides
    @Singleton
    fun provideCookieJar(store: CookieStore): SharedCookieJar = SharedCookieJar(store)

    @Provides
    @Singleton
    fun provideHttpClient(
        @ApplicationContext context: Context,
        cookieJar: SharedCookieJar,
    ): OkHttpClient {
        val cache = Cache(File(context.cacheDir, CACHE_DIR), HttpClients.CACHE_SIZE_BYTES)
        return HttpClients.create(cache, cookieJar)
    }

    @Provides
    @Singleton
    fun provideRequestQueue(client: OkHttpClient): RequestQueue = RequestQueue(OkHttpTransport(client), queueScope())

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
    ): NetworkMonitor = ConnectivityNetworkMonitor(context.getSystemService(ConnectivityManager::class.java))

    /** The queue's app-lifetime scope. The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks. */
    private fun queueScope(dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope {
        val job = SupervisorJob()
        return CoroutineScope(job + dispatcher)
    }
}
