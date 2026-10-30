package app.pocketcp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.inNavigationOrder
import app.pocketcp.core.network.NetworkMonitor
import app.pocketcp.shell.PocketCpApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity: edge-to-edge, hosting the app shell with every contributed [FeatureEntry],
 * live on the user's settings and the network state.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var featureEntries: Set<@JvmSuppressWildcards FeatureEntry>

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val entries = featureEntries.inNavigationOrder()
        val isOnline = networkMonitor.isOnline
        setContent { PocketCpApp(entries, settingsRepository.settings, isOnline, onDarkTheme = ::matchSystemBars) }
    }

    /** Light or dark bar icons to match the app's theme, which the user may force against the system's. */
    private fun matchSystemBars(dark: Boolean) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
