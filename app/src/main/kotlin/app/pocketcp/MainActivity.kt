package app.pocketcp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.inNavigationOrder
import app.pocketcp.shell.PocketCpApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The single activity: edge-to-edge, hosting the app shell with every contributed [FeatureEntry]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var featureEntries: Set<@JvmSuppressWildcards FeatureEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val entries = featureEntries.inNavigationOrder()
        setContent { PocketCpApp(entries) }
    }
}
