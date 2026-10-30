package app.pocketcp.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.R
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import app.pocketcp.core.navigation.inNavigationOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A destination of a disabled optional module is gone from the bar and the rail (spec §15). */
@RunWith(AndroidJUnit4::class)
class ShellModulesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** A Stats destination between Problemset and Profile. */
    private val statsEntry: FeatureEntry =
        object : FeatureEntry by PlaceholderEntry(
            TopLevelDestination("stats", R.string.nav_profile, shellTestEntries.first().destination.icon, 25),
            R.string.placeholder_profile,
        ) {
            override val module = OptionalModule.Stats
        }

    private val entries = (shellTestEntries + statsEntry).inNavigationOrder()

    private val enabled = UserSettings()
    private val disabled = UserSettings(disabledModules = setOf(OptionalModule.Stats))

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun phone_enabledModule_isInTheBottomBar() {
        rule.setContent { PocketCpApp(entries, enabled) }

        rule.onNodeWithTag(BOTTOM_BAR_TAG).assertExists()
        rule.onNodeWithTag("nav_stats").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun phone_disabledModule_isAbsentFromTheBottomBar() {
        rule.setContent { PocketCpApp(entries, disabled) }

        rule.onNodeWithTag(BOTTOM_BAR_TAG).assertExists()
        rule.onNodeWithTag("nav_stats").assertDoesNotExist()
        rule.onNodeWithTag("nav_profile").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tablet_enabledModule_isInTheRail() {
        rule.setContent { PocketCpApp(entries, enabled) }

        rule.onNodeWithTag(RAIL_TAG).assertExists()
        rule.onNodeWithTag("nav_stats").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tablet_disabledModule_isAbsentFromTheRail() {
        rule.setContent { PocketCpApp(entries, disabled) }

        rule.onNodeWithTag(RAIL_TAG).assertExists()
        rule.onNodeWithTag("nav_stats").assertDoesNotExist()
        rule.onNodeWithTag("nav_profile").assertIsDisplayed()
    }
}
