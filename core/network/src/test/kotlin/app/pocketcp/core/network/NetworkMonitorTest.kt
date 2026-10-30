package app.pocketcp.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

/** [ConnectivityNetworkMonitor] on Robolectric's shadow `ConnectivityManager`, driving its registered callbacks. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkMonitorTest {
    private val connectivityManager =
        RuntimeEnvironment.getApplication().getSystemService(ConnectivityManager::class.java)
    private val shadow = shadowOf(connectivityManager)
    private val monitor = ConnectivityNetworkMonitor(connectivityManager)
    private val wifi = ShadowNetwork.newInstance(WIFI_ID)
    private val cellular = ShadowNetwork.newInstance(CELLULAR_ID)

    @Test
    fun `initial state is online when the active network has validated internet`() =
        runTest {
            shadow.setNetworkCapabilities(connectivityManager.activeNetwork, capabilities(validated = true))

            assertEquals(listOf(true), collect())
        }

    @Test
    fun `initial state is offline without an active network`() =
        runTest {
            shadow.setDefaultNetworkActive(false)

            assertEquals(listOf(false), collect())
        }

    @Test
    fun `initial state is offline when the active network is not validated`() =
        runTest {
            shadow.setNetworkCapabilities(connectivityManager.activeNetwork, capabilities(validated = false))

            assertEquals(listOf(false), collect())
        }

    @Test
    fun `losing the network goes offline and regaining it goes online`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()

            changed(wifi, capabilities(validated = true))
            callbacks().forEach { it.onLost(wifi) }
            runCurrent()
            changed(wifi, capabilities(validated = true))

            assertEquals(listOf(false, true, false, true), values)
        }

    @Test
    fun `duplicate callbacks are deduplicated`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()

            changed(wifi, capabilities(validated = true))
            changed(wifi, capabilities(validated = true))
            changed(cellular, capabilities(validated = true))
            callbacks().forEach { it.onLost(wifi) } // a late loss of the previous default network
            runCurrent()

            assertEquals(listOf(false, true), values)
        }

    @Test
    fun `capabilities without VALIDATED are offline`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()

            changed(wifi, capabilities(validated = true))
            changed(wifi, capabilities(validated = false))

            assertEquals(listOf(false, true, false), values)
        }

    @Test
    fun `the callback is unregistered when collection stops`() =
        runTest {
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { monitor.isOnline.collect {} }
            assertEquals(1, callbacks().size)

            job.cancel()
            runCurrent()

            assertTrue(callbacks().isEmpty())
        }

    /** Collects [ConnectivityNetworkMonitor.isOnline] eagerly for the rest of the test. */
    private fun TestScope.collect(): List<Boolean> {
        val values = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.isOnline.toList(values) }
        runCurrent()
        return values
    }

    private fun TestScope.changed(
        network: Network,
        capabilities: NetworkCapabilities,
    ) {
        callbacks().forEach { it.onCapabilitiesChanged(network, capabilities) }
        runCurrent()
    }

    private fun callbacks(): Set<ConnectivityManager.NetworkCallback> = shadow.networkCallbacks

    private fun capabilities(validated: Boolean): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        val shadowCapabilities = Shadow.extract<ShadowNetworkCapabilities>(capabilities)
        shadowCapabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (validated) shadowCapabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return capabilities
    }

    private companion object {
        const val WIFI_ID = 100
        const val CELLULAR_ID = 101
    }
}
