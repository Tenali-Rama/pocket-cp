package app.pocketcp.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import app.pocketcp.core.ui.LocalClock
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

private val ShotNow: Instant = Instant.parse("2026-10-31T12:00:00Z")
private val ShotClock: Clock = Clock.fixed(ShotNow, ZoneOffset.UTC)

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) {
        CompositionLocalProvider(LocalClock provides ShotClock) { ProvideWindowLayout(content) }
    }
}

private val LargeFont = ScreenshotConfig(fontScale = 1.5f)

private object NoActions : HomeActions {
    override fun refresh() = Unit
}

private fun contest(
    id: Int,
    name: String,
    phase: ContestPhase,
    start: Instant,
    length: Duration = Duration.ofHours(2),
) = Contest(ContestId(id), name, phase, start, length, isGym = false, kind = ContestKind.Cf)

/** One running and six upcoming contests, so the shots also show the limit of five. */
private val ShotContests: List<Contest> =
    listOf(
        contest(2280, "Codeforces Round 1133 (Div. 1)", ContestPhase.Running, ShotNow.minus(Duration.ofMinutes(47))),
        contest(2281, "Codeforces Round 1134 (Div. 2)", ContestPhase.Upcoming, ShotNow.plusSeconds(5_025)),
        contest(
            2282,
            "Educational Codeforces Round 185 (Rated for Div. 2)",
            ContestPhase.Upcoming,
            ShotNow.plus(Duration.ofHours(29)),
        ),
        contest(2283, "Codeforces Round 1135 (Div. 3)", ContestPhase.Upcoming, ShotNow.plus(Duration.ofDays(2))),
        contest(
            2284,
            "Codeforces Global Round 31",
            ContestPhase.Upcoming,
            ShotNow.plus(Duration.ofDays(4)),
            Duration.ofHours(3),
        ),
        contest(2285, "Codeforces Round 1136 (Div. 4)", ContestPhase.Upcoming, ShotNow.plus(Duration.ofDays(6))),
        contest(2286, "Codeforces Round 1137 (Div. 2)", ContestPhase.Upcoming, ShotNow.plus(Duration.ofDays(9))),
    )

private fun state(
    data: List<Contest>? = ShotContests,
    error: JudgeError? = null,
    refreshing: Boolean = false,
) = homeUiState(
    Cached(data, fetchedAt = data?.let { ShotNow }, refreshing = refreshing, error = error),
    ShotNow,
    userRefreshing = false,
)

/**
 * `home_<device>_<theme>`: the guest card, 1 running and the next 5 upcoming contests (one list on phone and
 * foldable; guest card and Running | Upcoming on tablet and desktop). Phone dark variants: `home_noContests` (no
 * running or upcoming contest), `home_error` (no cache, offline), `home_staleError` (cache plus a failed refresh),
 * `home_loading` (no cache yet) and `home_font150`.
 */
class HomeScreenshotTest : ScreenshotTest() {
    @Test
    fun home() = captureMatrix("home", themeWrapper = PocketCpThemeWrapper) { Screen(state()) }

    @Test
    fun noContests() = phoneDark("home_noContests") { state(data = emptyList()) }

    @Test
    fun error() = phoneDark("home_error") { state(data = null, error = JudgeError.Offline()) }

    @Test
    fun staleError() = phoneDark("home_staleError") { state(error = JudgeError.Offline()) }

    @Test
    fun loading() = phoneDark("home_loading") { state(data = null, refreshing = true) }

    @Test
    fun largeFont() = captureSingle("home_font150", LargeFont, PocketCpThemeWrapper) { Screen(state()) }

    private fun phoneDark(
        name: String,
        state: () -> HomeUiState,
    ) = captureSingle(name, ScreenshotConfig(), PocketCpThemeWrapper) { Screen(state()) }
}

@Composable
private fun Screen(state: HomeUiState) = HomeScreen(state, ShotNow, NoActions)
