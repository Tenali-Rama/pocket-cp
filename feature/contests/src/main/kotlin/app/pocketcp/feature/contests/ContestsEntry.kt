package app.pocketcp.feature.contests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import app.pocketcp.core.ui.rememberNow
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Inject

/** The Contests tab. Binds the `contests` route, so it replaces the app's placeholder of the same route. */
class ContestsEntry
    @Inject
    constructor(
        private val repository: ContestRepository,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.contests_label, Icons.Filled.DateRange, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) {
                val viewModel =
                    viewModel { ContestsViewModel(repository, createSavedStateHandle(), Clock.systemUTC()) }
                ContestsRoute(viewModel)
            }
        }

        private companion object {
            const val ROUTE = "contests"
            const val ORDER = 10
        }
    }

/** [ContestsScreen] on [viewModel]'s state, with one ticking clock for every row's countdown. */
@Composable
private fun ContestsRoute(viewModel: ContestsViewModel) {
    val state by viewModel.state.collectAsState()
    ContestsScreen(state = state, now = rememberNow(), actions = viewModel)
}

/** Contributes [ContestsEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface ContestsModule {
    @Binds
    @IntoSet
    fun entry(entry: ContestsEntry): FeatureEntry
}
