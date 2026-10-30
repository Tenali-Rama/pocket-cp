package app.pocketcp.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/** The Settings tab: last in the navigation, always shown. */
class SettingsEntry
    @Inject
    constructor(
        private val repository: SettingsRepository,
        private val installedModules: Set<@JvmSuppressWildcards OptionalModule>,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.settings_label, Icons.Filled.Settings, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) {
                val viewModel = viewModel { SettingsViewModel(repository, installedModules) }
                val state by viewModel.state.collectAsState()
                SettingsRoute(state, viewModel)
            }
        }

        private companion object {
            const val ROUTE = "settings"
            const val ORDER = 40
        }
    }

/** Contributes [SettingsEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {
    @Binds
    @IntoSet
    fun entry(entry: SettingsEntry): FeatureEntry
}
