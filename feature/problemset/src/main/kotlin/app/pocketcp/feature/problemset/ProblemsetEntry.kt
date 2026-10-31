package app.pocketcp.feature.problemset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/** The Problemset tab. Binds the `problemset` route, so it replaces the app's placeholder of the same route. */
class ProblemsetEntry
    @Inject
    constructor(
        private val problems: ProblemRepository,
        private val contests: ContestRepository,
        private val settings: SettingsRepository,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.problemset_label, Icons.AutoMirrored.Filled.List, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) {
                val viewModel =
                    viewModel { ProblemsetViewModel(problems, contests, settings, createSavedStateHandle()) }
                ProblemsetRoute(viewModel)
            }
        }

        private companion object {
            const val ROUTE = "problemset"
            const val ORDER = 20
        }
    }

/** [ProblemsetScreen] on [viewModel]'s state and search text. */
@Composable
private fun ProblemsetRoute(viewModel: ProblemsetViewModel) {
    val state by viewModel.state.collectAsState()
    ProblemsetScreen(state = state, searchText = viewModel.searchText, actions = viewModel)
}

/** Contributes [ProblemsetEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface ProblemsetModule {
    @Binds
    @IntoSet
    fun entry(entry: ProblemsetEntry): FeatureEntry
}
