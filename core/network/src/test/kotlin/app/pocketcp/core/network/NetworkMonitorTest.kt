package app.pocketcp.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * [ConnectivityNetworkMonitor] on Robolectric's shadow `ConnectivityManager`, driving its registered callbacks.
 * The monitor runs on a test dispatcher of the test's scheduler, so the offline debounce is virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkMonitorTest {
    private val connectivityManager =
        RuntimeEnvironment.getApplication().getSystemService(ConnectivityManager::class.java)
    private val shadow = shadowOf(connectivityManager)
    private val wifi = ShadowNetwork.newInstance(WIFI_ID)
    private val cellular = ShadowNetwork.newInstance(CELLULAR_ID)
    private val offlineDelay = ConnectivityNetworkMonitor.OFFLINE_DELAY

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
    fun `an initial offline state is emitted at once, without the debounce`() =
        runTest {
            shadow.setDefaultNetworkActive(false)

            val values = collect()

            assertEquals(listOf(false), values)
            assertEquals(0L, testScheduler.currentTime)
        }

    @Test
    fun `losing the network goes offline and regaining it goes online`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()

            changed(wifi, capabilities(validated = true))
            lost(wifi)
            advanceTimeBy(offlineDelay)
            runCurrent()
            changed(wifi, capabilities(validated = true))

            assertEquals(listOf(false, true, false, true), values)
        }

    @Test
    fun `losing and regaining the network within the debounce emits nothing new`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()
            changed(wifi, capabilities(validated = true))

            lost(wifi)
            advanceTimeBy(offlineDelay - 1.milliseconds)
            runCurrent()
            changed(cellular, capabilities(validated = true))
            advanceTimeBy(offlineDelay * 2)
            runCurrent()

            assertEquals(listOf(false, true), values)
        }

    @Test
    fun `a loss that lasts the debounce emits offline`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()
            changed(wifi, capabilities(validated = true))

            lost(wifi)
            advanceTimeBy(offlineDelay - 1.milliseconds)
            runCurrent()
            assertEquals(listOf(false, true), values)

            advanceTimeBy(1.milliseconds)
            runCurrent()
            assertEquals(listOf(false, true, false), values)
        }

    @Test
    fun `duplicate callbacks are deduplicated`() =
        runTest {
            shadow.setDefaultNetworkActive(false)
            val values = collect()

            changed(wifi, capabilities(validated = true))
            changed(wifi, capabilities(validated = true))
            changed(cellular, capabilities(validated = true))
            lost(wifi) // a late loss of the previous default network
            advanceTimeBy(offlineDelay * 2)
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
            advanceTimeBy(offlineDelay)
            runCurrent()

            assertEquals(listOf(false, true, false), values)
        }

    @Test
    fun `the callback is unregistered when collection stops`() =
        runTest {
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { monitor().isOnline.collect {} }
            runCurrent()
            assertEquals(1, callbacks().size)

            job.cancel()
            runCurrent()

            assertTrue(callbacks().isEmpty())
        }

    @Test
    fun `collection runs on the injected dispatcher`() =
        runTest {
            val dispatcher = QueuedDispatcher()
            val monitor = ConnectivityNetworkMonitor(connectivityManager, dispatcher)
            val values = mutableListOf<Boolean>()

            // The collector is unconfined: without flowOn the callback would be registered right here.
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { monitor.isOnline.toList(values) }
            assertTrue(callbacks().isEmpty())

            dispatcher.runQueued()
            assertEquals(1, callbacks().size)
            assertEquals(1, values.size)

            job.cancel()
            dispatcher.runQueued()
            assertTrue(callbacks().isEmpty())
        }

    /** A monitor on the test's scheduler, so `delay` in the debounce is virtual time. */
    private fun TestScope.monitor(): NetworkMonitor =
        ConnectivityNetworkMonitor(connectivityManager, StandardTestDispatcher(testScheduler), offlineDelay)

    /** Collects [ConnectivityNetworkMonitor.isOnline] eagerly for the rest of the test. */
    private fun TestScope.collect(): List<Boolean> {
        val values = mutableListOf<Boolean>()
        val monitor = monitor()
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

    private fun TestScope.lost(network: Network) {
        callbacks().forEach { it.onLost(network) }
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

    /** Runs nothing until [runQueued]: proves which work was dispatched to it. */
    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            queue.addLast(block)
        }

        fun runQueued() {
            while (queue.isNotEmpty()) queue.removeFirst().run()
        }
    }

    private companion object {
        const val WIFI_ID = 100
        const val CELLULAR_ID = 101
    }
}
