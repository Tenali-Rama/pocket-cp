package app.pocketcp.core.data

import app.pocketcp.core.network.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Keeps the list caches warm (spec §3 prefetch): when the app starts online, and whenever the connection comes
 * back, it refreshes the regular contests and the problemset. Refreshes are not forced, so a fresh cache costs
 * no request.
 *
 * The repositories come through [Provider]s and are resolved on [scope], so [start] on the main thread doesn't
 * build the HTTP client and the database during app start.
 */
@Singleton
class CacheWarmer internal constructor(
    private val networkMonitor: NetworkMonitor,
    private val contests: Provider<ContestRepository>,
    private val problems: Provider<ProblemRepository>,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        networkMonitor: NetworkMonitor,
        contests: Provider<ContestRepository>,
        problems: Provider<ProblemRepository>,
    ) : this(networkMonitor, contests, problems, warmerScope())

    private val started = AtomicBoolean(false)

    /** Starts watching the connection for the app's lifetime. Later calls do nothing. */
    fun start() {
        if (started.getAndSet(true)) return
        scope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .filter { online -> online }
                .collect { warm() }
        }
    }

    /** Both refreshes at once; they share the request queue, which spaces them. */
    private suspend fun warm() =
        coroutineScope {
            launch { guarded { contests.get().refresh(gym = false) } }
            launch { guarded { problems.get().refresh() } }
        }

    /**
     * Repositories record a refresh failure instead of throwing; anything that still escapes is dropped here, so
     * it can neither crash the app's scope nor stop warming on the next reconnect.
     */
    @Suppress("TooGenericExceptionCaught", "RethrowCaughtException", "SwallowedException")
    private suspend fun guarded(refresh: suspend () -> Unit) {
        try {
            refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (ignored: Exception) {
            // Nothing to show: the lists show their own refresh state when opened.
        }
    }
}

/** The warmer's app-lifetime scope. The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks. */
private fun warmerScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope =
    CoroutineScope(SupervisorJob() + dispatcher)
