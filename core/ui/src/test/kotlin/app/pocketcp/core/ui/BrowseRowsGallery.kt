package app.pocketcp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.designsystem.UiDensity
import app.pocketcp.core.designsystem.component.PcpCard
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.screenshot.ThemeWrapper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/** The fixed "now" of every row test and screenshot. */
internal val TestNow: Instant = Instant.parse("2026-10-31T12:00:00Z")

/** A clock stopped at [TestNow] in UTC, so dates and countdowns don't depend on when or where tests run. */
internal val TestClock: Clock = Clock.fixed(TestNow, ZoneOffset.UTC)

/** Windows at least this wide (Material 3 medium width) show contests and problems side by side. */
private val WideMinWidth = 600.dp

private val TwoHours: Duration = Duration.ofHours(2)

/** One contest per row state: upcoming (over a day away), running, finished, unknown, and no start time. */
internal val SampleContests =
    listOf(
        Contest(
            ContestId(2001),
            "Codeforces Round 990 (Div. 2)",
            ContestPhase.Upcoming,
            start =
                TestNow.plus(
                    Duration
                        .ofDays(2)
                        .plusHours(3)
                        .plusMinutes(4)
                        .plusSeconds(5),
                ),
            duration = TwoHours,
            isGym = false,
        ),
        Contest(
            ContestId(2000),
            "Educational Codeforces Round 175 (Rated for Div. 2)",
            ContestPhase.Running,
            start = TestNow.minus(Duration.ofMinutes(45)),
            duration = TwoHours,
            isGym = false,
        ),
        Contest(
            ContestId(1999),
            "Codeforces Round 989 (Div. 1 + Div. 2)",
            ContestPhase.Finished,
            start = Instant.parse("2026-10-24T14:35:00Z"),
            duration = Duration.ofMinutes(135),
            isGym = false,
        ),
        Contest(ContestId(1998), "Testing Round 21", ContestPhase.Unknown, TestNow, TwoHours, isGym = false),
        Contest(ContestId(2002), "Codeforces Round 991 (Div. 3)", ContestPhase.Upcoming, null, TwoHours, isGym = false),
    )

/** Rated with tags, unrated, and a long name with many tags. */
internal val SampleProblems =
    listOf(
        Problem(ProblemId(1900, "A"), "Cover in Water", 800, listOf("constructive algorithms", "greedy"), 45_123),
        Problem(ProblemId(1234, "B1"), "Social Network (easy version)", null, listOf("implementation"), 31_002),
        Problem(
            ProblemId(1896, "H2"),
            "Cyclic Hamming (Hard Version) with an extra long name that wraps",
            3500,
            listOf("brute force", "divide and conquer", "fft", "math", "number theory", "strings"),
            87,
        ),
    )

/** [PocketCpTheme] dark or light at [density], with [clock] as the UI clock. */
internal fun browseThemeWrapper(
    density: UiDensity = UiDensity.Comfortable,
    clock: Clock = TestClock,
): ThemeWrapper =
    { dark, content ->
        PocketCpTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, density = density) {
            CompositionLocalProvider(LocalClock provides clock, content = content)
        }
    }

/**
 * The refresh banner over every contest row state and three problem rows (the second highlighted).
 * Without [showContests] only the banner and the problem rows, so 1.5× text fits a phone.
 */
@Composable
internal fun BrowseRowsGallery(
    hideTags: Boolean = false,
    hideRating: Boolean = false,
    showContests: Boolean = true,
) {
    val spacing = PocketCpTheme.spacing
    Column(
        Modifier.fillMaxSize().background(PocketCpTheme.colors.background).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        RefreshErrorBanner(JudgeError.Offline(), onRetry = {}, modifier = Modifier.fillMaxWidth())
        BoxWithConstraints {
            if (!showContests) {
                ProblemCard(hideTags, hideRating, Modifier.fillMaxWidth())
            } else if (maxWidth >= WideMinWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xl)) {
                    ContestCard(Modifier.weight(1f))
                    ProblemCard(hideTags, hideRating, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    ContestCard(Modifier.fillMaxWidth())
                    ProblemCard(hideTags, hideRating, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ContestCard(modifier: Modifier) {
    // A fixed now: a ticking rememberNow() would keep the screenshot from ever settling.
    PcpCard(modifier) {
        SampleContests.forEach { ContestRow(it, TestNow, onClick = {}) }
    }
}

@Composable
private fun ProblemCard(
    hideTags: Boolean,
    hideRating: Boolean,
    modifier: Modifier,
) {
    PcpCard(modifier) {
        SampleProblems.forEachIndexed { index, problem ->
            ProblemRow(problem, hideTags, hideRating, highlighted = index == 1, onClick = {})
        }
    }
}
