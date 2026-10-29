package app.pocketcp.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsPropertiesAndroid
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.navigation.inNavigationOrder
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The shell's navigation state on a phone-sized window (Robolectric's default, a bottom bar). */
@RunWith(AndroidJUnit4::class)
class ShellStateTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val entries = PlaceholderModule.placeholders().inNavigationOrder()

    @Test
    fun selectedDestination_survivesStateRestoration() {
        val tester = StateRestorationTester(rule)
        tester.setContent { PocketCpApp(entries) }
        rule.onNodeWithTag("nav_problemset").performClick()
        rule.onNodeWithTag("nav_problemset").assertIsSelected()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithTag("nav_problemset").assertIsSelected()
        rule.onNodeWithTag("nav_home").assertIsNotSelected()
    }

    @Test
    fun startsOnHome() {
        rule.setContent { PocketCpApp(entries) }

        rule.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test
    fun back_fromNonHomeDestination_returnsToHome_notToThePreviousTab() {
        rule.setContent { PocketCpApp(entries) }
        rule.onNodeWithTag("nav_contests").performClick()
        rule.onNodeWithTag("nav_settings").performClick()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithTag("nav_home").assertIsSelected()
        rule.onNodeWithTag("nav_contests").assertIsNotSelected()
        rule.onNodeWithTag("nav_settings").assertIsNotSelected()
        assertFalse(rule.activity.isFinishing)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabThenEnter_onHardwareKeyboard_switchesDestination() {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            PocketCpApp(entries)
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("nav_home").requestFocus()
        rule.onNodeWithTag("nav_home").assertIsFocused()

        rule.onNodeWithTag("nav_home").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag("nav_contests").assertIsFocused()
        rule.onNodeWithTag("nav_contests").performKeyInput { pressKey(Key.Enter) }

        rule.onNodeWithTag("nav_contests").assertIsSelected()
        rule.onNodeWithTag("nav_home").assertIsNotSelected()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun root_exposesTestTagsAsResourceIds() {
        rule.setContent { PocketCpApp(entries) }

        rule
            .onNodeWithTag(ROOT_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsPropertiesAndroid.TestTagsAsResourceId, true))
    }
}
