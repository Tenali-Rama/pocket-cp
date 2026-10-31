package app.pocketcp.feature.problemset

import androidx.compose.runtime.Composable
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) { ProvideWindowLayout(content) }
}

private val LargeFont = ScreenshotConfig(fontScale = 1.5f)

private object NoActions : ProblemsetActions {
    override fun search(text: String) = Unit

    override fun sortBy(sort: ProblemSort) = Unit

    override fun setHideTags(hidden: Boolean) = Unit

    override fun setHideRatings(hidden: Boolean) = Unit

    override fun toggleTag(tag: String) = Unit

    override fun toggleDivision(division: ContestDivision) = Unit

    override fun setRatingRange(
        min: Int,
        max: Int,
    ) = Unit

    override fun clearFilters() = Unit

    override fun pickRandom() = Unit

    override fun refresh() = Unit
}

private fun sample(
    contestId: Int,
    index: String,
    name: String,
    rating: Int,
    vararg tags: String,
) = Problem(ProblemId(contestId, index), name, rating, tags.toList(), solvedCount = rating * 7)

/** A wider problemset than the fixture's three, so the filtered list beside the panel is not empty. */
private val FilterSample =
    listOf(
        sample(1905, "C", "Largest Subsequence", 1400, "greedy", "math", "strings"),
        sample(1904, "B", "Collecting Game", 1100, "binary search", "greedy", "math", "sortings"),
        sample(1903, "C", "Theofanis' Nightmare", 1400, "constructive algorithms", "greedy", "math"),
        sample(1901, "C", "Add, Divide and Floor", 1400, "constructive algorithms", "greedy", "math"),
        sample(1900, "C", "Anji's Binary Tree", 1300, "dfs and similar", "dp", "trees"),
        sample(1899, "D", "Yarik and Musical Notes", 1300, "hashing", "math", "number theory"),
        sample(1898, "B", "Milena and Admirer", 1500, "greedy", "math"),
        sample(1896, "C", "Matching Arrays", 1400, "binary search", "constructive algorithms", "greedy", "sortings"),
        sample(1895, "D", "XOR Construction", 1900, "bitmasks", "constructive algorithms", "data structures", "math"),
    )

/** Two tags and 1200–1600: four of [FilterSample] match. */
private val SampleFilter = ProblemFilter(tags = setOf("greedy", "math"), ratingMin = 1200, ratingMax = 1600)

private fun state(
    data: List<Problem>? = FixtureProblems,
    options: ProblemsetOptions = ProblemsetOptions(),
    error: JudgeError? = null,
    refreshing: Boolean = false,
    pick: RandomPick = RandomPick(),
) = problemsetUiState(
    Cached(data, fetchedAt = null, refreshing = refreshing, error = error),
    options,
    userRefreshing = false,
    pick = pick,
)

/**
 * `problemset_<device>_<theme>`: `problemset.problems.json` (phone: search and sort chips above the list; wider:
 * search pane | list). Phone dark variants: `problemset_search` (contest id `2274`: the fixture has no contest
 * 1900), `problemset_hiddenSpoilers` (tags and ratings hidden), `problemset_empty` (a query that matches nothing),
 * `problemset_error` (no cache, offline), `problemset_staleError` (cache plus a failed refresh),
 * `problemset_loading` (no cache yet), `problemset_font150` and `problemset_random` (2274A picked and highlighted).
 * `problemsetFilters_<device>_<theme>`: [SampleFilter] over [FilterSample], in the open sheet (phone, foldable) or
 * the panel beside the list (tablet, desktop); `problemsetFilters_font150` the sheet at 1.5x.
 */
class ProblemsetScreenshotTest : ScreenshotTest() {
    @Test
    fun problemset() = captureMatrix("problemset", themeWrapper = PocketCpThemeWrapper) { Screen(state()) }

    @Test
    fun search() = phoneDark("problemset_search") { state(options = ProblemsetOptions(query = "2274")) }

    @Test
    fun hiddenSpoilers() =
        phoneDark("problemset_hiddenSpoilers") {
            state(options = ProblemsetOptions(hideTags = true, hideRatings = true))
        }

    @Test
    fun empty() = phoneDark("problemset_empty") { state(options = ProblemsetOptions(query = "dijkstra")) }

    @Test
    fun error() = phoneDark("problemset_error") { state(data = null, error = JudgeError.Offline()) }

    @Test
    fun staleError() = phoneDark("problemset_staleError") { state(error = JudgeError.Offline()) }

    @Test
    fun loading() = phoneDark("problemset_loading") { state(data = null, refreshing = true) }

    @Test
    fun filters() =
        captureMatrix("problemsetFilters", themeWrapper = PocketCpThemeWrapper) {
            Screen(state(FilterSample, ProblemsetOptions(filter = SampleFilter)), filtersOpen = true)
        }

    @Test
    fun filtersLargeFont() =
        captureSingle("problemsetFilters_font150", LargeFont, PocketCpThemeWrapper) {
            Screen(state(FilterSample, ProblemsetOptions(filter = SampleFilter)), filtersOpen = true)
        }

    @Test
    fun random() = phoneDark("problemset_random") { state(pick = RandomPick("2274A", count = 1)) }

    @Test
    fun largeFont() = captureSingle("problemset_font150", LargeFont, PocketCpThemeWrapper) { Screen(state()) }

    private fun phoneDark(
        name: String,
        state: () -> ProblemsetUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(state()) }
}

@Composable
private fun Screen(
    state: ProblemsetUiState,
    filtersOpen: Boolean = false,
) = ProblemsetScreen(state, state.options.query, NoActions, filtersOpen = filtersOpen)
