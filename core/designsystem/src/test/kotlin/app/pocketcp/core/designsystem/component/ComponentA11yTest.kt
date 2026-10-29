package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Touch targets (spec §16) and hardware-keyboard activation (spec §2, §5) of the design-system controls. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class ComponentA11yTest {
    @get:Rule
    val rule = createComposeRule()

    private val minTouchTarget = 48.dp

    @Test
    fun iconButtonTouchTargetIsAtLeast48dp() {
        setThemedContent {
            PcpIconButton(Icons.Filled.Refresh, contentDescription = "Refresh", onClick = {})
        }

        rule.onNodeWithContentDescription("Refresh").assertTouchTargetAtLeast(minTouchTarget)
    }

    @Test
    fun filterChipTouchTargetIsAtLeast48dp() {
        setThemedContent {
            Column {
                PcpFilterChip("A", selected = true, onClick = {})
                PcpFilterChip("B", selected = false, onClick = {})
            }
        }

        rule.onNodeWithText("A").assertTouchTargetAtLeast(minTouchTarget)
        rule.onNodeWithText("B").assertTouchTargetAtLeast(minTouchTarget)
    }

    @Test
    fun enterKeyClicksFocusedButtons() {
        val clicks = mutableMapOf<String, Int>()
        val inputModeManager =
            setThemedContent {
                Column {
                    PcpPrimaryButton("Primary", onClick = { clicks.merge("Primary", 1, Int::plus) })
                    PcpSecondaryButton("Secondary", onClick = { clicks.merge("Secondary", 1, Int::plus) })
                    PcpTextButton("Text", onClick = { clicks.merge("Text", 1, Int::plus) })
                    PcpIconButton(
                        Icons.Filled.Refresh,
                        contentDescription = "Icon",
                        onClick = { clicks.merge("Icon", 1, Int::plus) },
                    )
                }
            }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        listOf("Primary", "Secondary", "Text").forEach { label ->
            rule.onNodeWithText(label).pressEnterWhenFocused()
        }
        rule.onNodeWithContentDescription("Icon").pressEnterWhenFocused()

        rule.runOnIdle {
            assertEquals(mapOf("Primary" to 1, "Secondary" to 1, "Text" to 1, "Icon" to 1), clicks)
        }
    }

    @Test
    fun disabledButtonCannotTakeFocusOrClick() {
        var clicks = 0
        val inputModeManager =
            setThemedContent {
                PcpPrimaryButton("Off", onClick = { clicks++ }, enabled = false)
            }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        rule
            .onNodeWithText("Off")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    /** Sets [content] inside [PocketCpTheme]; returns the window's input mode manager. */
    private fun setThemedContent(content: @Composable () -> Unit): InputModeManager {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            PocketCpTheme(content = content)
        }
        return inputModeManager
    }

    @OptIn(ExperimentalTestApi::class)
    private fun SemanticsNodeInteraction.pressEnterWhenFocused() {
        requestFocus()
        assertIsFocused()
        performKeyInput { pressKey(Key.Enter) }
    }

    private fun SemanticsNodeInteraction.assertTouchTargetAtLeast(min: Dp) {
        val node = fetchSemanticsNode()
        val bounds = node.touchBoundsInRoot
        with(node.layoutInfo.density) {
            assertTrue("touch width ${bounds.width.toDp()} < $min", bounds.width.toDp() >= min)
            assertTrue("touch height ${bounds.height.toDp()} < $min", bounds.height.toDp() >= min)
        }
    }
}
