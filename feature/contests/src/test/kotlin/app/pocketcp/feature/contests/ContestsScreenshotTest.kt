package app.pocketcp.feature.contests

import androidx.compose.runtime.Composable
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

private val LargeFont = ScreenshotConfig(fontScale = 1.5f)

/**
 * `contests_<device>_<theme>`: `contest.list.json` at the moment it was recorded, nothing opened (phone: chips
 * above the list; medium: chips and list | the first contest's detail; expanded: filter pane | list | detail).
 * Phone dark variants: `contests_filtered` (Div. 2 + CF), `contests_empty` (a filter that leaves nothing),
 * `contests_error` (no cache, offline), `contests_staleError` (cache plus a failed refresh), `contests_loading`
 * (no cache yet) and `contests_font150`.
 */
class ContestsScreenshotTest : ScreenshotTest() {
    @Test
    fun contests() =
        captureMatrix("contests", themeWrapper = PocketCpThemeWrapper) {
            Screen(listState(), detailState(UPCOMING_ID, opened = false))
        }

    @Test
    fun filtered() =
        phoneDark("contests_filtered") {
            listState(filter = ContestFilter(setOf(ContestDivision.Div2), setOf(ContestKind.Cf)))
        }

    @Test
    fun empty() {
        phoneDark("contests_empty") { listState(filter = ContestFilter(divisions = setOf(ContestDivision.Div1))) }
    }

    @Test
    fun error() = phoneDark("contests_error") { listState(data = null, error = JudgeError.Offline()) }

    @Test
    fun staleError() = phoneDark("contests_staleError") { listState(error = JudgeError.Offline()) }

    @Test
    fun loading() = phoneDark("contests_loading") { listState(data = null, refreshing = true) }

    @Test
    fun largeFont() = captureSingle("contests_font150", LargeFont, PocketCpThemeWrapper) { Screen(listState()) }

    private fun phoneDark(
        name: String,
        state: () -> ContestsUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(state()) }
}

@Composable
private fun Screen(
    state: ContestsUiState,
    detail: ContestDetailUiState = ContestDetailUiState(),
) = ContestsScreen(state, detail, FixtureNow, NoActions, NoDetailActions)
