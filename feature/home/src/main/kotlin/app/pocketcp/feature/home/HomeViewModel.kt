package app.pocketcp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.model.Contest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration

/** How long the list stays collected after the screen stops, so a rotation does not restart the flow. */
private const val STOP_TIMEOUT_MS = 5_000L

/** What the Home screen asks for. */
interface HomeActions {
    /** A refresh the user asked for (the refresh button or Retry): always fetches. */
    fun refresh()
}

/**
 * The Home screen's state: the running and next [UPCOMING_LIMIT] upcoming regular contests from [repository]'s
 * bounded `activeContests` view (never the full list), sectioned as of [clock]'s now. When a countdown reaches zero
 * the list is re-sectioned locally at once and a forced refresh is started, so the judge's phases catch up; until it
 * lands, Upcoming may show one fewer. Sectioning runs on [computeDispatcher], off the main thread. Built by
 * [HomeEntry] for its back stack entry (ADR 0012).
 */
class HomeViewModel(
    private val repository: ContestRepository,
    private val clock: Clock,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    HomeActions {
    private val userRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> =
        combine(repository.activeContests(upcomingLimit = UPCOMING_LIMIT), userRefreshing, ::Inputs)
            .transformLatest { inputs -> emitSectioned(inputs) }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    override fun refresh() {
        if (userRefreshing.value) return
        userRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.refresh(gym = false, force = true)
            } finally {
                userRefreshing.value = false
            }
        }
    }

    /**
     * Emits the state for [inputs] now, then again whenever a countdown reaches zero. `now` never lags the instant
     * waited for, so a delay that wakes a little early still flips the contest instead of waiting (and refreshing)
     * twice.
     */
    private suspend fun FlowCollector<HomeUiState>.emitSectioned(inputs: Inputs) {
        val contests = inputs.cached.data.orEmpty()
        var now = clock.instant()
        while (true) {
            emit(homeUiState(inputs.cached, now, inputs.userRefreshing))
            val next = nextCountdownEnd(contests, now) ?: return
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
            now = maxOf(clock.instant(), next)
            // In the view model's scope: the refresh's own cache update restarts this flow and must not cancel it.
            viewModelScope.launch { repository.refresh(gym = false, force = true) }
        }
    }

    private data class Inputs(
        val cached: Cached<List<Contest>>,
        val userRefreshing: Boolean,
    )
}
