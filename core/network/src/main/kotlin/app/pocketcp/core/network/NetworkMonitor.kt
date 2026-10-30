package app.pocketcp.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Whether the device can reach the internet right now. */
interface NetworkMonitor {
    /** `true` while the default network has validated internet access. Emits the current state first, then changes. */
    val isOnline: Flow<Boolean>
}

/**
 * [NetworkMonitor] on [ConnectivityManager]'s default-network callback: online means the default network has
 * both `INTERNET` and `VALIDATED`. Each collector registers its own callback until it stops collecting.
 *
 * The connectivity calls run on [dispatcher], never on the collector's (main) thread. Going offline is reported
 * only once it has lasted [offlineDelay], so a switch of the default network doesn't flash the offline banner;
 * going online, and an offline initial state, are reported at once.
 */
class ConnectivityNetworkMonitor(
    private val connectivityManager: ConnectivityManager,
    private val dispatcher: CoroutineDispatcher,
    private val offlineDelay: Duration = OFFLINE_DELAY,
) : NetworkMonitor {
    override val isOnline: Flow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    /** The network the last capabilities came from; a late `onLost` of an older one is ignored. */
                    private var current: Network? = null

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        current = network
                        trySend(capabilities.hasValidatedInternet())
                    }

                    override fun onLost(network: Network) {
                        if (current == null || network == current) {
                            current = null
                            trySend(false)
                        }
                    }
                }
            trySend(currentlyOnline())
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.conflate()
            .distinctUntilChanged()
            .debounceOffline()
            .flowOn(dispatcher)

    /** Holds back a `false` after the first item until it has lasted [offlineDelay]; a `true` cancels it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Boolean>.debounceOffline(): Flow<Boolean> =
        withIndex()
            .transformLatest { (index, online) ->
                if (!online && index > 0) delay(offlineDelay)
                emit(online)
            }.distinctUntilChanged()

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(network)?.hasValidatedInternet() == true
    }

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    companion object {
        /** How long a loss must last before [isOnline] reports it. */
        val OFFLINE_DELAY: Duration = 2.seconds
    }
}
