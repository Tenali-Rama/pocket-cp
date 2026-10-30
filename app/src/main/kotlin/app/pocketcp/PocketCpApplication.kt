package app.pocketcp

import android.app.Application
import app.pocketcp.core.data.CacheWarmer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Hilt's root component lives here. Starts warming the list caches (spec §3 prefetch). */
@HiltAndroidApp
class PocketCpApplication : Application() {
    @Inject
    lateinit var cacheWarmer: CacheWarmer

    override fun onCreate() {
        super.onCreate()
        cacheWarmer.start()
    }
}
