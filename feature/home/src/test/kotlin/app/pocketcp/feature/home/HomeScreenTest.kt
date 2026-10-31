package app.pocketcp.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

private val Now: Instant = Instant.parse("2026-10-31T12:00:00Z")

private val Upcoming =
    Contest(
        ContestId(2278),
        "Codeforces Round 1131 (Div. 3)",
        ContestPhase.Upcoming,
        Now.plusSeconds(90),
        Duration.ofHours(2),
        isGym = false,
    )

/** The guest card, the empty state and the user's refresh, on [HomeScreen] with a fixed state. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var refreshes = 0
    private val actions =
        object : HomeActions {
            override fun refresh() {
                refreshes++
            }
        }

    @Test
    fun guestCard_isShown_withoutAnAction() {
        show(listOf(Upcoming))
        rule.onNodeWithTag(GUEST_TAG).assertHasNoClickAction()
        rule.onNodeWithText(rule.activity.getString(R.string.home_guest)).assertExists()
    }

    @Test
    fun upcomingContest_countsDownToItsStart() {
        show(listOf(Upcoming))
        // The row's phase label says "Upcoming" too: match only the section header.
        rule.onNode(hasText(rule.activity.getString(R.string.home_section_upcoming)) and isHeading()).assertExists()
        rule.onNodeWithTag("contest_2278").assertExists()
        rule.onNodeWithText("Starts in 00:01:30", substring = true).assertExists()
    }

    @Test
    fun noRunningOrUpcoming_showsTheEmptyState_andTheGuestCard() {
        show(emptyList())
        rule.onNodeWithText(rule.activity.getString(R.string.home_empty_title)).assertExists()
        rule.onNodeWithTag(GUEST_TAG).assertExists()
    }

    @Test
    fun refreshButton_asksForARefresh() {
        show(listOf(Upcoming))
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.home_refresh)).performClick()
        assertEquals(1, refreshes)
    }

    private fun show(contests: List<Contest>) {
        val state = homeUiState(Cached(contests, Now, refreshing = false, error = null), Now, userRefreshing = false)
        val width = WindowWidthClass.Compact
        val layout = WindowLayout(width, WindowHeightClass.Medium, DevicePosture.Flat, navigationTypeOf(width))
        rule.setContent {
            PocketCpTheme(ThemeMode.Dark) {
                CompositionLocalProvider(LocalWindowLayout provides layout) { HomeScreen(state, Now, actions) }
            }
        }
    }
}
