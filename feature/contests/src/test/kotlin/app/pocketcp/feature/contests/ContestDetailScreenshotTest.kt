package app.pocketcp.feature.contests

import androidx.compose.runtime.Composable
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/**
 * `contestDetail_<device>_<theme>`: finished fixture contest 2274 opened, with its problem 2274A (phone: the detail
 * page; wider: list | detail with its row highlighted). Phone dark variants: `contestDetail_upcoming` (2278: its
 * countdown and the not-yet-added message), `contestDetail_empty` (2272: finished, no problems),
 * `contestDetail_loading` (no problemset cached yet), `contestDetail_error` (no problemset, offline) and
 * `contestDetail_font150`.
 */
class ContestDetailScreenshotTest : ScreenshotTest() {
    @Test
    fun contestDetail() {
        captureMatrix("contestDetail", themeWrapper = PocketCpThemeWrapper) { Screen(detailState(WITH_PROBLEMS_ID)) }
    }

    @Test
    fun upcoming() = phoneDark("contestDetail_upcoming") { detailState(UPCOMING_ID) }

    @Test
    fun empty() = phoneDark("contestDetail_empty") { detailState(NO_PROBLEMS_ID) }

    @Test
    fun loading() {
        phoneDark("contestDetail_loading") { detailState(WITH_PROBLEMS_ID, problems = null, problemsRefreshing = true) }
    }

    @Test
    fun error() =
        phoneDark("contestDetail_error") {
            detailState(WITH_PROBLEMS_ID, problems = null, problemsError = JudgeError.Offline())
        }

    @Test
    fun largeFont() =
        captureSingle("contestDetail_font150", ScreenshotConfig(fontScale = 1.5f), PocketCpThemeWrapper) {
            Screen(detailState(WITH_PROBLEMS_ID))
        }

    private fun phoneDark(
        name: String,
        detail: () -> ContestDetailUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(detail()) }
}

@Composable
private fun Screen(detail: ContestDetailUiState) {
    ContestsScreen(listState(), detail, FixtureNow, NoActions, NoDetailActions)
}
