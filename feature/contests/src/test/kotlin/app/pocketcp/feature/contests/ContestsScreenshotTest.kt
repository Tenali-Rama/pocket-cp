package app.pocketcp.feature.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import app.pocketcp.core.ui.LocalClock
import org.junit.Test

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) {
        CompositionLocalProvider(LocalClock provides FixtureClock) { ProvideWindowLayout(content) }
    }
}

private val LargeFont = ScreenshotConfig(fontScale = 1.5f)

private object NoActions : ContestsActions {
    override fun toggleDivision(division: ContestDivision) = Unit

    override fun toggleKind(kind: ContestKind) = Unit

    override fun clearFilters() = Unit

    override fun refresh() = Unit
}

private fun state(
    data: List<Contest>? = FixtureContests,
    filter: ContestFilter = ContestFilter(),
    error: JudgeError? = null,
    refreshing: Boolean = false,
) = contestsUiState(
    Cached(data, fetchedAt = data?.let { FixtureNow }, refreshing = refreshing, error = error),
    filter,
    FixtureNow,
    userRefreshing = false,
)

/**
 * `contests_<device>_<theme>`: `contest.list.json` at the moment it was recorded (phone: chips above the list;
 * wider: filter pane | list). Phone dark variants: `contests_filtered` (Div. 2 + CF), `contests_empty` (a filter
 * that leaves nothing), `contests_error` (no cache, offline), `contests_staleError` (cache plus a failed refresh),
 * `contests_loading` (no cache yet) and `contests_font150`.
 */
class ContestsScreenshotTest : ScreenshotTest() {
    @Test
    fun contests() = captureMatrix("contests", themeWrapper = PocketCpThemeWrapper) { Screen(state()) }

    @Test
    fun filtered() =
        phoneDark("contests_filtered") {
            state(filter = ContestFilter(setOf(ContestDivision.Div2), setOf(ContestKind.Cf)))
        }

    @Test
    fun empty() = phoneDark("contests_empty") { state(filter = ContestFilter(divisions = setOf(ContestDivision.Div1))) }

    @Test
    fun error() = phoneDark("contests_error") { state(data = null, error = JudgeError.Offline()) }

    @Test
    fun staleError() = phoneDark("contests_staleError") { state(error = JudgeError.Offline()) }

    @Test
    fun loading() = phoneDark("contests_loading") { state(data = null, refreshing = true) }

    @Test
    fun largeFont() = captureSingle("contests_font150", LargeFont, PocketCpThemeWrapper) { Screen(state()) }

    private fun phoneDark(
        name: String,
        state: () -> ContestsUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(state()) }
}

@Composable
private fun Screen(state: ContestsUiState) = ContestsScreen(state, FixtureNow, NoActions)
