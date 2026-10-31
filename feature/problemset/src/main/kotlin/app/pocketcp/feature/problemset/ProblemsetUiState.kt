package app.pocketcp.feature.problemset

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem

/** What the list area shows (ADR 0011). */
enum class ProblemsetStatus {
    /** No cached data yet: a skeleton. */
    Loading,

    /** No cached data and the refresh failed: a full error with Retry. */
    Error,

    /** Data, but nothing matches the query and filters (or the judge lists no problems). */
    Empty,

    /** At least one problem to show. */
    Loaded,
}

/** The search, filter and display options the list is computed from. */
@Immutable
data class ProblemsetOptions(
    val query: String = "",
    val sort: ProblemSort = ProblemSort.Newest,
    val hideTags: Boolean = false,
    val hideRatings: Boolean = false,
    val filter: ProblemFilter = ProblemFilter(),
) {
    /** The order applied: a rating order falls back to [ProblemSort.Newest] while ratings are hidden. */
    val effectiveSort: ProblemSort get() = if (hideRatings && sort.usesRating) ProblemSort.Newest else sort

    /** The orders offered: without the rating orders while ratings are hidden. */
    val sortChoices: List<ProblemSort>
        get() = if (hideRatings) ProblemSort.entries.filterNot { it.usesRating } else ProblemSort.entries
}

/**
 * The last random pick: [display] of the problem to highlight, and [count], how many picks were made, so the list
 * scrolls to each new pick even when it lands on the highlighted problem again.
 */
@Immutable
data class RandomPick(
    val display: String? = null,
    val count: Int = 0,
)

/**
 * The Problemset screen. [problems] match [options]' query and filter, in its effective order. [tags] are every
 * tag in the cached problemset (and every selected one), sorted, for the tag filter. [pick] is the highlighted
 * random problem. [error] is the last refresh's failure: a banner over the list when there is data, the full
 * error state otherwise. [userRefreshing] is a refresh the user asked for; a background refresh never shows an
 * indicator.
 */
@Immutable
data class ProblemsetUiState(
    val status: ProblemsetStatus = ProblemsetStatus.Loading,
    val problems: List<Problem> = emptyList(),
    val options: ProblemsetOptions = ProblemsetOptions(),
    val error: JudgeError? = null,
    val userRefreshing: Boolean = false,
    val tags: List<String> = emptyList(),
    val pick: RandomPick = RandomPick(),
) {
    /** "Random" needs at least one problem to pick from. */
    val canPickRandom: Boolean get() = problems.isNotEmpty()
}

/**
 * The screen state for [cached] problems searched, filtered and sorted per [options], with each cached contest's
 * division in [divisionOf] (by contest id) and [pick] highlighted.
 */
internal fun problemsetUiState(
    cached: Cached<List<Problem>>,
    options: ProblemsetOptions,
    userRefreshing: Boolean,
    divisionOf: Map<Int, ContestDivision> = emptyMap(),
    pick: RandomPick = RandomPick(),
): ProblemsetUiState {
    val data = cached.data
    if (data == null) {
        val failed = cached.error != null && !cached.refreshing
        return ProblemsetUiState(
            status = if (failed) ProblemsetStatus.Error else ProblemsetStatus.Loading,
            options = options,
            error = cached.error,
            userRefreshing = userRefreshing,
            pick = pick,
        )
    }
    val problems = searchProblems(data, ProblemQuery(options.query), options.effectiveSort, options.filter, divisionOf)
    return ProblemsetUiState(
        status = if (problems.isEmpty()) ProblemsetStatus.Empty else ProblemsetStatus.Loaded,
        problems = problems,
        options = options,
        error = cached.error,
        userRefreshing = userRefreshing,
        // Selected tags stay offered even when no cached problem has one, so they can be cleared.
        tags = data.flatMapTo(sortedSetOf()) { it.tags }.apply { addAll(options.filter.tags) }.toList(),
        pick = pick,
    )
}
