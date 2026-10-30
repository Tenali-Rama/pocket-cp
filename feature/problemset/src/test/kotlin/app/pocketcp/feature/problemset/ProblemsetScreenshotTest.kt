package app.pocketcp.feature.problemset

import androidx.compose.runtime.Composable
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
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

    override fun refresh() = Unit
}

private fun state(
    data: List<Problem>? = FixtureProblems,
    options: ProblemsetOptions = ProblemsetOptions(),
    error: JudgeError? = null,
    refreshing: Boolean = false,
) = problemsetUiState(Cached(data, fetchedAt = null, refreshing = refreshing, error = error), options, false)

/**
 * `problemset_<device>_<theme>`: `problemset.problems.json` (phone: search and sort chips above the list; wider:
 * search pane | list). Phone dark variants: `problemset_search` (contest id `2274`: the fixture has no contest
 * 1900), `problemset_hiddenSpoilers` (tags and ratings hidden), `problemset_empty` (a query that matches nothing),
 * `problemset_error` (no cache, offline), `problemset_staleError` (cache plus a failed refresh),
 * `problemset_loading` (no cache yet) and `problemset_font150`.
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
    fun largeFont() = captureSingle("problemset_font150", LargeFont, PocketCpThemeWrapper) { Screen(state()) }

    private fun phoneDark(
        name: String,
        state: () -> ProblemsetUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(state()) }
}

@Composable
private fun Screen(state: ProblemsetUiState) = ProblemsetScreen(state, state.options.query, NoActions)
