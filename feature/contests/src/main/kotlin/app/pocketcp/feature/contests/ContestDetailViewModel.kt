package app.pocketcp.feature.contests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.Problem
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration

/** How long the detail stays collected after the screen stops, so a rotation does not restart the flow. */
private const val DETAIL_STOP_TIMEOUT_MS = 5_000L

/** Saved-state key of the opened contest's id (or `null` once closed), so it survives process death. */
internal const val KEY_SELECTED = "contests_selected"

/** The `contests/{id}` route's argument: the contest to open when no selection was saved yet. */
internal const val ARG_ID = "id"

/** What the contest detail asks for. */
interface ContestDetailActions {
    /** Opens [id]: its page on compact width, the detail pane beside the list on wider windows. */
    fun open(id: ContestId)

    /** Closes the opened contest (Back or the back arrow on compact width). */
    fun close()

    /** A problemset refresh the user asked for (Retry): always fetches. */
    fun retryProblems()
}

/**
 * The contest detail's state: the opened contest (kept in [savedState]), or on wide windows the default the route
 * sets with [showByDefault], from [contests], its problems from [problems] and the spoiler flags from [settings].
 * Showing a contest triggers a non-forced problemset refresh (spec §3 prefetch; the stale rule decides). The state
 * is rebuilt when the contest starts or ends ([nextPhaseChange]), as of [clock], on [computeDispatcher].
 */
class ContestDetailViewModel(
    private val contests: ContestRepository,
    private val problems: ProblemRepository,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    ContestDetailActions {
    private val fallback = MutableStateFlow<ContestId?>(null)

    init {
        // A first visit to `contests/{id}` opens that contest; afterwards the saved selection wins.
        if (!savedState.contains(KEY_SELECTED)) savedState[KEY_SELECTED] = savedState.get<Int>(ARG_ID)
    }

    private val selection: Flow<Selection> =
        combine(savedState.getStateFlow<Int?>(KEY_SELECTED, null), fallback) { selected, default ->
            val opened = selected?.let(::ContestId)
            Selection(opened, opened ?: default)
        }.distinctUntilChanged()

    init {
        selection
            .map { it.shown }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { viewModelScope.launch { problems.refresh() } }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ContestDetailUiState> =
        selection
            .flatMapLatest { selection ->
                if (selection.shown == null) {
                    flowOf(ContestDetailUiState(selected = selection.opened))
                } else {
                    combine(contests.contests(gym = false), problems.problemset(), settings.settings) { c, p, s ->
                        Inputs(selection, c, p, s)
                    }.transformLatest { inputs -> emitTimed(inputs) }
                }
            }.flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(DETAIL_STOP_TIMEOUT_MS), ContestDetailUiState())

    override fun open(id: ContestId) {
        savedState[KEY_SELECTED] = id.value
    }

    override fun close() {
        savedState.set<Int>(KEY_SELECTED, null)
    }

    override fun retryProblems() {
        viewModelScope.launch { problems.refresh(force = true) }
    }

    /** The contest shown when none is opened: the list's first on wide windows, `null` on compact width. */
    fun showByDefault(id: ContestId?) {
        fallback.value = id
    }

    /** Emits the state for [inputs] now, then again whenever the shown contest starts or ends. */
    private suspend fun FlowCollector<ContestDetailUiState>.emitTimed(inputs: Inputs) {
        val contest = inputs.contests.data?.find { it.id == inputs.selection.shown }
        while (true) {
            val now = clock.instant()
            emit(
                contestDetailUiState(
                    selected = inputs.selection.opened,
                    shown = inputs.selection.shown,
                    contests = inputs.contests,
                    problemset = inputs.problemset,
                    settings = inputs.settings,
                    now = now,
                ),
            )
            val next = nextPhaseChange(listOfNotNull(contest), now) ?: return
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
        }
    }

    private data class Selection(
        val opened: ContestId?,
        val shown: ContestId?,
    )

    private data class Inputs(
        val selection: Selection,
        val contests: Cached<List<Contest>>,
        val problemset: Cached<List<Problem>>,
        val settings: UserSettings,
    )
}
