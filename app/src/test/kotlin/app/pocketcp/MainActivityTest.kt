package app.pocketcp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the real app (production Hilt graph) on Robolectric. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val navTags = listOf("nav_home", "nav_contests", "nav_problemset", "nav_profile", "nav_settings")

    /** The shell draws its first frame once DataStore has read the settings, off the main thread. */
    @Before
    fun waitForSettings() {
        composeRule.waitUntil(SETTINGS_TIMEOUT_MS) { composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun launch_showsShellWithEveryPlaceholder_homeSelected() {
        navTags.forEach { composeRule.onNodeWithTag(it).assertIsDisplayed() }
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
        composeRule.onNodeWithTag("nav_settings").assertIsNotSelected()
    }

    @Test
    fun hiltContributedEntries_areShownInNavigationOrder() {
        val lefts =
            navTags.map {
                composeRule
                    .onNodeWithTag(it)
                    .fetchSemanticsNode()
                    .boundsInRoot.left
            }

        assertEquals(lefts.sorted(), lefts)
        assertEquals(navTags.size, lefts.distinct().size)
    }

    @Test
    fun launch_runsInHiltApplication() {
        assertTrue(composeRule.activity.application is PocketCpApplication)
    }

    private companion object {
        const val SETTINGS_TIMEOUT_MS = 5_000L
    }
}
