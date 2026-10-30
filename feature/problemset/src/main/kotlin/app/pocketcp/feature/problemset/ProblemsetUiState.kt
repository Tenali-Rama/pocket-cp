package app.pocketcp.feature.problemset

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem

/** What the list area shows (ADR 0011). */
enum class ProblemsetStatus {
    /** No cached data yet: a skeleton. */
    Loading,

    /** No cached data and the refresh failed: a full error with Retry. */
    Error,

    /** Data, but nothing matches the query (or the judge lists no problems). */
    Empty,

    /** At least one problem to show. */
    Loaded,
}

/** The search and display options the list is computed from. */
@Immutable
data class ProblemsetOptions(
    val query: String = "",
    val sort: ProblemSort = ProblemSort.Newest,
    val hideTags: Boolean = false,
    val hideRatings: Boolean = false,
) {
    /** The order applied: a rating order falls back to [ProblemSort.Newest] while ratings are hidden. */
    val effectiveSort: ProblemSort get() = if (hideRatings && sort.usesRating) ProblemSort.Newest else sort

    /** The orders offered: without the rating orders while ratings are hidden. */
    val sortChoices: List<ProblemSort>
        get() = if (hideRatings) ProblemSort.entries.filterNot { it.usesRating } else ProblemSort.entries
}

/**
 * The Problemset screen. [problems] match [options]' query, in its effective order. [error] is the last refresh's
 * failure: a banner over the list when there is data, the full error state otherwise. [userRefreshing] is a
 * refresh the user asked for; a background refresh never shows an indicator.
 */
@Immutable
data class ProblemsetUiState(
    val status: ProblemsetStatus = ProblemsetStatus.Loading,
    val problems: List<Problem> = emptyList(),
    val options: ProblemsetOptions = ProblemsetOptions(),
    val error: JudgeError? = null,
    val userRefreshing: Boolean = false,
)

/** The screen state for [cached] problems searched and sorted per [options]. */
internal fun problemsetUiState(
    cached: Cached<List<Problem>>,
    options: ProblemsetOptions,
    userRefreshing: Boolean,
): ProblemsetUiState {
    val data = cached.data
    if (data == null) {
        val failed = cached.error != null && !cached.refreshing
        return ProblemsetUiState(
            status = if (failed) ProblemsetStatus.Error else ProblemsetStatus.Loading,
            options = options,
            error = cached.error,
            userRefreshing = userRefreshing,
        )
    }
    val problems = searchProblems(data, ProblemQuery(options.query), options.effectiveSort)
    return ProblemsetUiState(
        status = if (problems.isEmpty()) ProblemsetStatus.Empty else ProblemsetStatus.Loaded,
        problems = problems,
        options = options,
        error = cached.error,
        userRefreshing = userRefreshing,
    )
}
