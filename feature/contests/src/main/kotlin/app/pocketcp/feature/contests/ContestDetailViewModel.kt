package app.pocketcp.feature.contests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
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

/** Saved-state key of the opened problem's index in the opened contest (or `null`), so it survives process death. */
internal const val KEY_PROBLEM = "contests_problem"

/** The `contests/{id}` route's argument: the contest to open when no selection was saved yet. */
internal const val ARG_ID = "id"

/** What the contest detail asks for. */
interface ContestDetailActions {
    /**
     * Opens [id]: its page on compact width, the detail pane beside the list on wider windows. A contest other than
     * the opened one closes the opened problem.
     */
    fun open(id: ContestId)

    /** Closes the opened contest and its problem (Back or the back arrow on compact width). */
    fun close()

    /** Opens [id]'s statement, and its contest: a page on compact width, beside the contest on wider windows. */
    fun openProblem(id: ProblemId)

    /** Closes the opened problem, back to its contest (Back or the back arrow). */
    fun closeProblem()

    /** A problemset refresh the user asked for (Retry): always fetches. */
    fun retryProblems()
}

/**
 * The contest detail's state: the opened contest (kept in [savedState]), or on wide windows the default the route
 * sets with [showByDefault], from [contests], its problems from [problems] and the spoiler flags from [settings].
 * Showing a contest triggers a non-forced problemset refresh (spec §3 prefetch; the stale rule decides). The state
 * is rebuilt when the contest starts or ends ([nextPhaseChange]), as of [clock], on [computeDispatcher]. Once the
 * shown contest has problems, their statements are prefetched in the statement language through [statements]
 * (spec §3), once per contest, problem set and language. The opened problem is kept in [savedState] too.
 */
@Suppress("LongParameterList") // Four sources, the saved state and two test seams; none of them belong together.
class ContestDetailViewModel(
    private val contests: ContestRepository,
    private val problems: ProblemRepository,
    private val statements: StatementRepository,
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

    private val selection: Flow<DetailSelection> =
        combine(
            savedState.getStateFlow<Int?>(KEY_SELECTED, null),
            savedState.getStateFlow<String?>(KEY_PROBLEM, null),
            fallback,
        ) { selected, problem, default ->
            val opened = selected?.let(::ContestId)
            DetailSelection(opened, opened ?: default, problemOf(selected, problem))
        }.distinctUntilChanged()

    /** The prefetches already asked for, so each contest's problem set is asked for once. */
    private val prefetched = mutableSetOf<StatementPrefetch>()

    init {
        selection
            .map { it.shown }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { viewModelScope.launch { problems.refresh() } }
            .launchIn(viewModelScope)
    }

    init {
        prefetchStatements(computeDispatcher)
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(DETAIL_STOP_TIMEOUT_MS), restoredState())

    override fun open(id: ContestId) {
        if (savedState.get<Int>(KEY_SELECTED) != id.value) savedState.set<String>(KEY_PROBLEM, null)
        savedState[KEY_SELECTED] = id.value
    }

    override fun close() {
        savedState.set<String>(KEY_PROBLEM, null)
        savedState.set<Int>(KEY_SELECTED, null)
    }

    override fun openProblem(id: ProblemId) {
        // Contest first, so no state pairs this index with another contest.
        open(ContestId(id.contestId))
        savedState[KEY_PROBLEM] = id.index
    }

    override fun closeProblem() {
        savedState.set<String>(KEY_PROBLEM, null)
    }

    override fun retryProblems() {
        viewModelScope.launch { problems.refresh(force = true) }
    }

    /** The contest shown when none is opened: the list's first on wide windows, `null` on compact width. */
    fun showByDefault(id: ContestId?) {
        fallback.value = id
    }

    /** The first state: the saved selection, so a restored page or statement shows from the first frame. */
    private fun restoredState(): ContestDetailUiState {
        val selected = savedState.get<Int>(KEY_SELECTED)
        val opened = selected?.let(::ContestId)
        return ContestDetailUiState(opened, opened, problem = problemOf(selected, savedState.get<String>(KEY_PROBLEM)))
    }

    /** Prefetches the shown contest's statements whenever its problem set or the statement language changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun prefetchStatements(computeDispatcher: CoroutineDispatcher) {
        selection
            .map { it.shown }
            .distinctUntilChanged()
            .flatMapLatest { shown -> shown?.let(::prefetchesOf) ?: emptyFlow() }
            .flowOn(computeDispatcher)
            .onEach { if (prefetched.add(it)) statements.prefetch(it.problems, it.language) }
            .launchIn(viewModelScope)
    }

    /** [contest]'s problems in the statement language whenever they change, while it has any. */
    private fun prefetchesOf(contest: ContestId): Flow<StatementPrefetch> =
        combine(problems.problemset(), settings.settings) { problemset, userSettings ->
            val ids =
                problemset.data
                    ?.let { problemsOf(contest, it) }
                    .orEmpty()
                    .map { it.id }
            StatementPrefetch(contest, ids, userSettings.statementLanguage)
        }.filter { it.problems.isNotEmpty() }.distinctUntilChanged()

    /** Emits the state for [inputs] now, then again whenever the shown contest starts or ends. */
    private suspend fun FlowCollector<ContestDetailUiState>.emitTimed(inputs: Inputs) {
        val contest = inputs.contests.data?.find { it.id == inputs.selection.shown }
        while (true) {
            val now = clock.instant()
            emit(contestDetailUiState(inputs.selection, inputs.contests, inputs.problemset, inputs.settings, now))
            val next = nextPhaseChange(listOfNotNull(contest), now) ?: return
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
        }
    }

    private data class StatementPrefetch(
        val contest: ContestId,
        val problems: List<ProblemId>,
        val language: StatementLanguage,
    )

    private data class Inputs(
        val selection: DetailSelection,
        val contests: Cached<List<Contest>>,
        val problemset: Cached<List<Problem>>,
        val settings: UserSettings,
    )
}

/** The problem at [index] of contest [contest], when both are set. */
private fun problemOf(
    contest: Int?,
    index: String?,
): ProblemId? = if (contest != null && index != null) ProblemId(contest, index) else null
