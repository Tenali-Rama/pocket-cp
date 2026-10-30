package app.pocketcp.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the device can reach the internet right now. */
interface NetworkMonitor {
    /** `true` while the default network has validated internet access. Emits the current state first, then changes. */
    val isOnline: Flow<Boolean>
}

/**
 * [NetworkMonitor] on [ConnectivityManager]'s default-network callback: online means the default network has
 * both `INTERNET` and `VALIDATED`. Each collector registers its own callback until it stops collecting.
 */
class ConnectivityNetworkMonitor(
    private val connectivityManager: ConnectivityManager,
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

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(network)?.hasValidatedInternet() == true
    }

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
