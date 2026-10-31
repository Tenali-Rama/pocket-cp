package app.pocketcp.feature.contests

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.LocalClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The screen's wiring to [ContestsActions] and [ContestDetailActions], and its panes per width, over
 * `contest.list.json` (and `problemset.problems.json`) at its recording time.
 */
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

    private val detailActions =
        object : ContestDetailActions {
            override fun open(id: ContestId) {
                calls += "open ${id.value}"
            }

            override fun close() {
                calls += "close"
            }

            override fun retryProblems() {
                calls += "retry problems"
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

    @Test
    fun tappingARow_opensItsContest() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithTag("contest_$NO_PROBLEMS_ID").performScrollTo().performClick()
        assertEquals(listOf("open $NO_PROBLEMS_ID"), calls)
    }

    @Test
    fun compactPage_showsTheProblems_andItsBackArrowCloses() {
        show(WindowWidthClass.Compact, detail = detailState(WITH_PROBLEMS_ID))
        rule.onNodeWithTag("problem_${WITH_PROBLEMS_ID}A").assertExists()
        rule.onNodeWithTag("contest_$WITH_PROBLEMS_ID").assertDoesNotExist()
        rule.onNodeWithContentDescription("Navigate back").performClick()
        assertEquals(listOf("close"), calls)
    }

    @Test
    fun compactPage_systemBackCloses() {
        show(WindowWidthClass.Compact, detail = detailState(WITH_PROBLEMS_ID))
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(listOf("close"), calls)
    }

    @Test
    fun compactList_ignoresTheWideDefault() {
        show(WindowWidthClass.Compact, detail = detailState(UPCOMING_ID, opened = false))
        rule.onNodeWithTag(CONTEST_DETAIL_TAG).assertDoesNotExist()
        rule.onNodeWithTag("contest_$UPCOMING_ID").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun wide_showsListAndDetail_withTheShownRowSelected() {
        show(WindowWidthClass.Expanded, detail = detailState(WITH_PROBLEMS_ID))
        rule.onNodeWithTag("contest_$WITH_PROBLEMS_ID").assertIsSelected()
        rule.onNodeWithTag("contest_$UPCOMING_ID").assertIsNotSelected()
        rule.onNodeWithTag("problem_${WITH_PROBLEMS_ID}A").assertExists()
    }

    @Test
    fun upcomingDetail_countsDown_andSaysProblemsComeLater() {
        show(WindowWidthClass.Medium, detail = detailState(UPCOMING_ID, opened = false))
        val inDetail = hasAnyAncestor(hasTestTag(CONTEST_DETAIL_TAG))
        rule.onNode(hasText("Starts in 1d 16:55:20", substring = true) and inDetail).assertExists()
        rule
            .onNodeWithText("Problems appear here once Codeforces adds them to the problemset")
            .assertExists()
    }

    @Test
    fun problemsError_retryForcesAProblemsetRefresh() {
        show(
            WindowWidthClass.Compact,
            detail = detailState(WITH_PROBLEMS_ID, problems = null, problemsError = JudgeError.Offline()),
        )
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry problems"), calls)
    }

    private fun show(
        width: WindowWidthClass,
        data: List<Contest>? = FixtureContests,
        error: JudgeError? = null,
        filter: ContestFilter = ContestFilter(),
        detail: ContestDetailUiState = ContestDetailUiState(),
    ) {
        val cached = Cached(data, FixtureNow, refreshing = false, error = error)
        val state = contestsUiState(cached, filter, FixtureNow, userRefreshing = false)
        val layout = WindowLayout(width, WindowHeightClass.Medium, DevicePosture.Flat, navigationTypeOf(width))
        rule.setContent {
            PocketCpTheme(ThemeMode.Dark) {
                CompositionLocalProvider(LocalWindowLayout provides layout, LocalClock provides FixtureClock) {
                    ContestsScreen(state, detail, FixtureNow, actions, detailActions)
                }
            }
        }
    }
}
