package app.pocketcp.feature.problemset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.Problem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** How long the list stays collected after the screen stops, so a rotation does not restart the flow. */
private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Saved-state keys, so the options survive process death: the search text, the sort (enum name), the filter
 * (selected tags, division enum names, the rating range as one `[min, max]` value so a change is one update) and
 * the random pick (problem display, pick count).
 */
internal const val KEY_QUERY = "problemset_query"
internal const val KEY_SORT = "problemset_sort"
internal const val KEY_TAGS = "problemset_tags"
internal const val KEY_DIVISIONS = "problemset_divisions"
internal const val KEY_RATING = "problemset_rating"
internal const val KEY_PICK = "problemset_pick"
internal const val KEY_PICK_COUNT = "problemset_pick_count"

/** What the Problemset screen asks for. */
interface ProblemsetActions {
    fun search(text: String)

    fun sortBy(sort: ProblemSort)

    fun setHideTags(hidden: Boolean)

    fun setHideRatings(hidden: Boolean)

    fun toggleTag(tag: String)

    fun toggleDivision(division: ContestDivision)

    /** Snapped to `ProblemFilter.RATING_STEP`s inside the full range. */
    fun setRatingRange(
        min: Int,
        max: Int,
    )

    /** Resets every filter (not the search or the sort). */
    fun clearFilters()

    /** Highlights a problem picked uniformly from the current list, which scrolls to it. Nothing when empty. */
    fun pickRandom()

    /** A refresh the user asked for (pull, the refresh button or Retry): always fetches. */
    fun refresh()
}

/**
 * The Problemset screen's state: the problemset from [problems], searched, filtered and sorted per the options kept
 * in [savedState], with the spoiler toggles of [settings]. Divisions come from [contests]' regular contests by
 * contest id. Searching and filtering run on [computeDispatcher], off the main thread; random picks draw from
 * [random]. Built by [ProblemsetEntry] for its back stack entry (ADR 0012).
 */
class ProblemsetViewModel(
    private val problems: ProblemRepository,
    contests: ContestRepository,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val random: Random = Random.Default,
) : ViewModel(),
    ProblemsetActions {
    private val userRefreshing = MutableStateFlow(false)

    /**
     * The search box's text, as Compose state updated synchronously on each keystroke: a text field fed from the
     * async [state] would lose keystrokes and jump its cursor.
     */
    var searchText: String by mutableStateOf(savedState[KEY_QUERY] ?: "")
        private set

    private val filter: Flow<ProblemFilter> =
        combine(
            savedState.getStateFlow(KEY_TAGS, ArrayList<String>()),
            savedState.getStateFlow(KEY_DIVISIONS, ArrayList<String>()),
            savedState.getStateFlow(KEY_RATING, FullRange),
        ) { tags, divisions, rating ->
            val (min, max) = rating.takeIf { it.size == 2 } ?: FullRange
            ProblemFilter(
                tags = tags.toSet(),
                ratingMin = min,
                ratingMax = max,
                divisions = divisions.mapNotNull { name -> ContestDivision.entries.find { it.name == name } }.toSet(),
            )
        }

    private val options: Flow<ProblemsetOptions> =
        combine(
            savedState.getStateFlow(KEY_QUERY, ""),
            savedState.getStateFlow(KEY_SORT, ProblemSort.Newest.name),
            settings.settings.map { it.hideProblemTags to it.hideProblemRatings }.distinctUntilChanged(),
            filter,
        ) { query, sort, (hideTags, hideRatings), filter ->
            ProblemsetOptions(
                query = query,
                sort = ProblemSort.entries.find { it.name == sort } ?: ProblemSort.Newest,
                hideTags = hideTags,
                hideRatings = hideRatings,
                filter = filter,
            )
        }

    /** Each cached regular contest's division by contest id; empty until contests are cached. */
    private val divisions: Flow<Map<Int, ContestDivision>> =
        contests
            .contests(gym = false)
            .map { cached -> cached.data.orEmpty().associate { it.id.value to it.division } }
            .distinctUntilChanged()

    private val pick: Flow<RandomPick> =
        combine(
            savedState.getStateFlow<String?>(KEY_PICK, null),
            savedState.getStateFlow(KEY_PICK_COUNT, 0),
            ::RandomPick,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ProblemsetUiState> =
        combine(problems.problemset(), options, userRefreshing, divisions, pick, ::Inputs)
            .mapLatest { problemsetUiState(it.cached, it.options, it.refreshing, it.divisions, it.pick) }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProblemsetUiState())

    override fun search(text: String) {
        searchText = text
        savedState[KEY_QUERY] = text
    }

    override fun sortBy(sort: ProblemSort) {
        savedState[KEY_SORT] = sort.name
    }

    override fun setHideTags(hidden: Boolean) {
        viewModelScope.launch { settings.setHideProblemTags(hidden) }
    }

    override fun setHideRatings(hidden: Boolean) {
        viewModelScope.launch { settings.setHideProblemRatings(hidden) }
    }

    override fun toggleTag(tag: String) = savedState.toggle(KEY_TAGS, tag)

    override fun toggleDivision(division: ContestDivision) = savedState.toggle(KEY_DIVISIONS, division.name)

    override fun setRatingRange(
        min: Int,
        max: Int,
    ) {
        val (low, high) = ProblemFilter.snapRange(min, max)
        savedState[KEY_RATING] = arrayListOf(low, high)
    }

    override fun clearFilters() {
        savedState[KEY_TAGS] = ArrayList<String>()
        savedState[KEY_DIVISIONS] = ArrayList<String>()
        savedState[KEY_RATING] = FullRange
    }

    override fun pickRandom() {
        val shown = state.value.problems
        if (shown.isEmpty()) return
        savedState[KEY_PICK] = shown[random.nextInt(shown.size)].id.display
        savedState[KEY_PICK_COUNT] = (savedState.get<Int>(KEY_PICK_COUNT) ?: 0) + 1
    }

    override fun refresh() {
        if (userRefreshing.value) return
        userRefreshing.value = true
        viewModelScope.launch {
            try {
                problems.refresh(force = true)
            } finally {
                userRefreshing.value = false
            }
        }
    }

    /** The values [state] is computed from. */
    private data class Inputs(
        val cached: Cached<List<Problem>>,
        val options: ProblemsetOptions,
        val refreshing: Boolean,
        val divisions: Map<Int, ContestDivision>,
        val pick: RandomPick,
    )
}

/** The unfiltered rating range, as kept under [KEY_RATING]. */
private val FullRange: ArrayList<Int> get() = arrayListOf(ProblemFilter.MIN_RATING, ProblemFilter.MAX_RATING)

/** Adds [value] to the string list under [key], or removes it when present. */
private fun SavedStateHandle.toggle(
    key: String,
    value: String,
) {
    val selected = get<ArrayList<String>>(key).orEmpty()
    set(key, ArrayList(if (value in selected) selected - value else selected + value))
}
