package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Behaviour of [PcpSwitchRow] and [PcpChoiceRow]: whole-row input, keyboard and roles. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
@OptIn(ExperimentalTestApi::class)
class ControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun switchRowCarriesSwitchRoleAndTogglesOnTap() {
        setKeyboardContent {
            var checked by remember { mutableStateOf(false) }
            PcpSwitchRow("Stress tester", checked = checked, onCheckedChange = { checked = it }, supporting = "Off")
        }
        val row = rule.onNodeWithText("Stress tester")

        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOff()
        row.performClick().assertIsOn()
    }

    @Test
    fun switchRowTogglesWithEnterAndSpace() {
        setKeyboardContent {
            var checked by remember { mutableStateOf(false) }
            PcpSwitchRow("AI", checked = checked, onCheckedChange = { checked = it })
        }
        val row = rule.onNodeWithText("AI")
        row.requestFocus()
        row.assertIsFocused()

        row.performKeyInput { pressKey(Key.Enter) }
        row.assertIsOn()
        row.performKeyInput { pressKey(Key.Spacebar) }
        row.assertIsOff()
    }

    @Test
    fun disabledSwitchRowIgnoresTaps() {
        val changes = mutableListOf<Boolean>()
        setKeyboardContent {
            PcpSwitchRow("Local compute", checked = true, onCheckedChange = { changes += it }, enabled = false)
        }

        rule
            .onNodeWithText("Local compute")
            .assertIsNotEnabled()
            .assertIsOn()
            .performClick()

        rule.runOnIdle { assertEquals(emptyList<Boolean>(), changes) }
    }

    @Test
    fun choiceRowsSelectOneOptionByTapOrEnter() {
        setKeyboardContent {
            var selected by remember { mutableStateOf("Default") }
            Column(Modifier.selectableGroup()) {
                listOf("Small", "Default", "Large").forEach { option ->
                    PcpChoiceRow(option, selected = option == selected, onClick = { selected = option })
                }
            }
        }
        rule
            .onNodeWithText("Default")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()

        rule.onNodeWithText("Small").performClick().assertIsSelected()
        rule.onNodeWithText("Default").assertIsNotSelected()

        val large = rule.onNodeWithText("Large")
        large.requestFocus()
        large.performKeyInput { pressKey(Key.Enter) }
        large.assertIsSelected()
        rule.onNodeWithText("Small").assertIsNotSelected()
    }

    /** Sets [content] inside [PocketCpTheme], with the window in keyboard input mode. */
    private fun setKeyboardContent(content: @Composable () -> Unit) {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            PocketCpTheme(content = content)
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
    }
}
