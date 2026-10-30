package app.pocketcp.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * Home, the start destination (order 0, ADR 0008). Binds the `home` route, so it replaces the app's placeholder of
 * the same route.
 */
class HomeEntry
    @Inject
    constructor(
        private val repository: ContestRepository,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.home_label, Icons.Filled.Home, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) {
                val viewModel = viewModel { HomeViewModel(repository, Clock.systemUTC()) }
                HomeRoute(viewModel)
            }
        }

        private companion object {
            const val ROUTE = "home"
            const val ORDER = 0
        }
    }

/** [HomeScreen] on [viewModel]'s state, with one ticking clock for every row's countdown. */
@Composable
private fun HomeRoute(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    HomeScreen(state = state, now = rememberNow(), actions = viewModel)
}

/** Contributes [HomeEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface HomeModule {
    @Binds
    @IntoSet
    fun entry(entry: HomeEntry): FeatureEntry
}
