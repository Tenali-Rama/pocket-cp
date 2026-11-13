package app.pocketcp.feature.contests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import app.pocketcp.core.problemview.ProblemPane
import app.pocketcp.core.problemview.ProblemViewModelFactory
import app.pocketcp.core.ui.rememberNow
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Inject

/**
 * The Contests tab. Binds the `contests` route, so it replaces the app's placeholder of the same route, and
 * `contests/{id}`: the same screen with contest `id` opened. A contest's problems open in `core/problemview`.
 */
class ContestsEntry
    @Inject
    constructor(
        private val contests: ContestRepository,
        private val problems: ProblemRepository,
        private val statements: StatementRepository,
        private val settings: SettingsRepository,
        private val problemViews: ProblemViewModelFactory,
    ) : FeatureEntry {
        override val destination =
            TopLevelDestination(ROUTE, R.string.contests_label, Icons.Filled.DateRange, order = ORDER)

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) { Route() }
            graph.composable(DETAIL_ROUTE, arguments = listOf(navArgument(ARG_ID) { type = NavType.IntType })) {
                Route()
            }
        }

        /** Both view models for the current back stack entry (ADR 0012), each with its own saved state. */
        @Composable
        private fun Route() {
            val list = viewModel { ContestsViewModel(contests, createSavedStateHandle(), Clock.systemUTC()) }
            val detail =
                viewModel {
                    ContestDetailViewModel(
                        contests,
                        problems,
                        statements,
                        settings,
                        createSavedStateHandle(),
                        Clock.systemUTC(),
                    )
                }
            ContestsRoute(list, detail) { id, onBack, modifier -> Problem(id, onBack, modifier) }
        }

        /** One problem view model per problem (ADR 0012 addendum), built by `core/problemview`. */
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
            const val ROUTE = "contests"
            const val DETAIL_ROUTE = "$ROUTE/{$ARG_ID}"
            const val ORDER = 10
        }
    }

/**
 * [ContestsScreen] on both view models' state, with one ticking clock for every countdown, and [problemPane] for
 * the opened problem. On wide windows the list's first contest is shown until the user opens one.
 */
@Composable
private fun ContestsRoute(
    list: ContestsViewModel,
    detail: ContestDetailViewModel,
    problemPane: ProblemPaneSlot,
) {
    val state by list.state.collectAsState()
    val detailState by detail.state.collectAsState()
    val wide = LocalWindowLayout.current.widthClass != WindowWidthClass.Compact
    val first =
        state.groups
            .firstOrNull()
            ?.contests
            ?.firstOrNull()
            ?.id
    LaunchedEffect(detail, wide, first) { detail.showByDefault(if (wide) first else null) }
    ContestsScreen(
        state = state,
        detail = detailState,
        now = rememberNow(),
        actions = list,
        detailActions = detail,
        problemPane = problemPane,
    )
}

/** Contributes [ContestsEntry] to the shell's `Set<FeatureEntry>`. */
@Module
@InstallIn(SingletonComponent::class)
interface ContestsModule {
    @Binds
    @IntoSet
    fun entry(entry: ContestsEntry): FeatureEntry
}
