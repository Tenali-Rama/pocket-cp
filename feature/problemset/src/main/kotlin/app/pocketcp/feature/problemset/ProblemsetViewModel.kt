package app.pocketcp.feature.problemset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.datastore.SettingsRepository
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

/** How long the list stays collected after the screen stops, so a rotation does not restart the flow. */
private const val STOP_TIMEOUT_MS = 5_000L

/** Saved-state keys: the search text and the sort (enum name), so they survive process death. */
internal const val KEY_QUERY = "problemset_query"
internal const val KEY_SORT = "problemset_sort"

/** What the Problemset screen asks for. */
interface ProblemsetActions {
    fun search(text: String)

    fun sortBy(sort: ProblemSort)

    fun setHideTags(hidden: Boolean)

    fun setHideRatings(hidden: Boolean)

    /** A refresh the user asked for (pull, the refresh button or Retry): always fetches. */
    fun refresh()
}

/**
 * The Problemset screen's state: the problemset from [problems], searched and sorted per the query and sort kept
 * in [savedState], with the spoiler toggles of [settings]. Searching and sorting run on [computeDispatcher], off
 * the main thread. Built by [ProblemsetEntry] for its back stack entry (ADR 0012).
 */
class ProblemsetViewModel(
    private val problems: ProblemRepository,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    ProblemsetActions {
    private val userRefreshing = MutableStateFlow(false)

    /**
     * The search box's text, as Compose state updated synchronously on each keystroke: a text field fed from the
     * async [state] would lose keystrokes and jump its cursor.
     */
    var searchText: String by mutableStateOf(savedState[KEY_QUERY] ?: "")
        private set

    private val options: Flow<ProblemsetOptions> =
        combine(
            savedState.getStateFlow(KEY_QUERY, ""),
            savedState.getStateFlow(KEY_SORT, ProblemSort.Newest.name),
            settings.settings.map { it.hideProblemTags to it.hideProblemRatings }.distinctUntilChanged(),
        ) { query, sort, (hideTags, hideRatings) ->
            ProblemsetOptions(
                query = query,
                sort = ProblemSort.entries.find { it.name == sort } ?: ProblemSort.Newest,
                hideTags = hideTags,
                hideRatings = hideRatings,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ProblemsetUiState> =
        combine(problems.problemset(), options, userRefreshing) { cached, options, refreshing ->
            Triple(cached, options, refreshing)
        }.mapLatest { (cached, options, refreshing) -> problemsetUiState(cached, options, refreshing) }
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
}
