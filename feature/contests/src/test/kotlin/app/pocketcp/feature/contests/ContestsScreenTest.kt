package app.pocketcp.feature.contests

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.adaptive.DevicePosture
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowHeightClass
import app.pocketcp.core.adaptive.WindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.adaptive.navigationTypeOf
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.LocalClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The screen's wiring to [ContestsActions], over `contest.list.json` at its recording time. */
@RunWith(AndroidJUnit4::class)
class ContestsScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions =
        object : ContestsActions {
            override fun toggleDivision(division: ContestDivision) {
                calls += "division ${division.name}"
            }

            override fun toggleKind(kind: ContestKind) {
                calls += "kind ${kind.name}"
            }

            override fun clearFilters() {
                calls += "clear"
            }

            override fun refresh() {
                calls += "refresh"
            }
        }

    @Test
    fun rows_showTheFixtureUpcomingContestCountingDown() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithTag("contest_2278").assertTextContains("Starts in 1d 16:55:20", substring = true)
        rule.onNodeWithTag("contest_2272").assertExists()
    }

    @Test
    fun chipRows_toggleTheirDivisionAndKind() = chipsToggle(WindowWidthClass.Compact)

    @Test
    fun filterPane_togglesItsDivisionAndKind() = chipsToggle(WindowWidthClass.Expanded)

    @Test
    fun refreshButton_refreshes() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithContentDescription("Refresh contests").performClick()
        assertEquals(listOf("refresh"), calls)
    }

    @Test
    fun errorState_retryRefreshes() {
        show(WindowWidthClass.Compact, data = null, error = JudgeError.Offline())
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("refresh"), calls)
    }

    @Test
    fun emptyFilteredState_clearsTheFilters() {
        show(WindowWidthClass.Compact, filter = ContestFilter(divisions = setOf(ContestDivision.Div1)))
        rule.onNodeWithText("Clear filters").performClick()
        assertEquals(listOf("clear"), calls)
    }

    private fun chipsToggle(width: WindowWidthClass) {
        show(width)
        rule.onNodeWithTag("contests_division_div2").performScrollTo().performClick()
        rule.onNodeWithTag("contests_kind_cf").performScrollTo().performClick()
        assertEquals(listOf("division Div2", "kind Cf"), calls)
    }

    private fun show(
        width: WindowWidthClass,
        data: List<Contest>? = FixtureContests,
        error: JudgeError? = null,
        filter: ContestFilter = ContestFilter(),
    ) {
        val cached = Cached(data, FixtureNow, refreshing = false, error = error)
        val state = contestsUiState(cached, filter, FixtureNow, userRefreshing = false)
        val layout = WindowLayout(width, WindowHeightClass.Medium, DevicePosture.Flat, navigationTypeOf(width))
        rule.setContent {
            PocketCpTheme(ThemeMode.Dark) {
                CompositionLocalProvider(LocalWindowLayout provides layout, LocalClock provides FixtureClock) {
                    ContestsScreen(state, FixtureNow, actions)
                }
            }
        }
    }
}
