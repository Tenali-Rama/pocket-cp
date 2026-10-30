package app.pocketcp.feature.contests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

/** Saved-state keys: the selected chips, as enum names, so they survive process death. */
internal const val KEY_DIVISIONS = "contests_divisions"
internal const val KEY_KINDS = "contests_kinds"

/** What the Contests screen asks for. */
interface ContestsActions {
    fun toggleDivision(division: ContestDivision)

    fun toggleKind(kind: ContestKind)

    fun clearFilters()

    /** A refresh the user asked for (pull, the refresh button or Retry): always fetches. */
    fun refresh()
}

/**
 * The Contests screen's state: regular contests (no gyms) from [repository], filtered by the chips kept in
 * [savedState], sectioned as of [clock]'s now and re-sectioned when an upcoming contest starts or ends.
 * Filtering and sorting run on [computeDispatcher], off the main thread. Built by [ContestsEntry] for its back
 * stack entry (ADR 0012).
 */
class ContestsViewModel(
    private val repository: ContestRepository,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    ContestsActions {
    private val userRefreshing = MutableStateFlow(false)

    private val filter: Flow<ContestFilter> =
        combine(
            savedState.getStateFlow(KEY_DIVISIONS, ArrayList<String>()),
            savedState.getStateFlow(KEY_KINDS, ArrayList<String>()),
        ) { divisions, kinds ->
            ContestFilter(
                divisions = divisions.mapNotNull { name -> ContestDivision.entries.find { it.name == name } }.toSet(),
                kinds = kinds.mapNotNull { name -> ContestKind.entries.find { it.name == name } }.toSet(),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ContestsUiState> =
        combine(repository.contests(gym = false), filter, userRefreshing, ::Inputs)
            .transformLatest { inputs -> emitSectioned(inputs) }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ContestsUiState())

    override fun toggleDivision(division: ContestDivision) = toggle(KEY_DIVISIONS, division.name)

    override fun toggleKind(kind: ContestKind) = toggle(KEY_KINDS, kind.name)

    override fun clearFilters() {
        savedState[KEY_DIVISIONS] = ArrayList<String>()
        savedState[KEY_KINDS] = ArrayList<String>()
    }

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

    private fun toggle(
        key: String,
        name: String,
    ) {
        val selected = savedState.get<ArrayList<String>>(key).orEmpty()
        savedState[key] = ArrayList(if (name in selected) selected - name else selected + name)
    }

    /** Emits the state for [inputs] now, then again at every instant a contest changes section. */
    private suspend fun FlowCollector<ContestsUiState>.emitSectioned(inputs: Inputs) {
        val contests = inputs.cached.data.orEmpty()
        while (true) {
            val now = clock.instant()
            emit(contestsUiState(inputs.cached, inputs.filter, now, inputs.userRefreshing))
            val next = nextPhaseChange(contests, now) ?: return
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
        }
    }

    private data class Inputs(
        val cached: Cached<List<Contest>>,
        val filter: ContestFilter,
        val userRefreshing: Boolean,
    )
}
