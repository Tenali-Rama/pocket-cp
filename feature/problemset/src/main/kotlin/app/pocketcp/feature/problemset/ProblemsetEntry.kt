package app.pocketcp.feature.problemset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import app.pocketcp.core.problemview.ProblemPane
import app.pocketcp.core.problemview.ProblemViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * The Problemset tab. Binds the `problemset` route, so it replaces the app's placeholder of the same route, and
 * `problemset/{id}` (`id` such as `2274A`): the same screen with that problem opened once (ADR 0016).
 */
class ProblemsetEntry
    @Inject
    constructor(
        private val problems: ProblemRepository,
        private val contests: ContestRepository,
        private val settings: SettingsRepository,
        private val problemViews: ProblemViewModelFactory,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.problemset_label, Icons.AutoMirrored.Filled.List, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) { Route() }
            graph.composable(DETAIL_ROUTE, arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) {
                Route()
            }
        }

        /** The list's view model for the current back stack entry (ADR 0012), and one view model per problem. */
        @Composable
        private fun Route() {
            val viewModel = viewModel { ProblemsetViewModel(problems, contests, settings, createSavedStateHandle()) }
            val state by viewModel.state.collectAsState()
            ProblemsetScreen(
                state = state,
                searchText = viewModel.searchText,
                actions = viewModel,
                problemPane = { id, onBack, modifier -> Problem(id, onBack, modifier) },
            )
        }

        @Composable
        private fun Problem(
            id: ProblemId,
            onBack: (() -> Unit)?,
            modifier: Modifier,
        ) {
            val viewModel = viewModel(key = id.display) { problemViews.create(id) }
            ProblemPane(viewModel, onBack, modifier)
        }

        private companion object {
            const val ROUTE = "problemset"
            const val DETAIL_ROUTE = "$ROUTE/{$ARG_ID}"
            const val ORDER = 20
        }
    }

/** Contributes [ProblemsetEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface ProblemsetModule {
    @Binds
    @IntoSet
    fun entry(entry: ProblemsetEntry): FeatureEntry
}
