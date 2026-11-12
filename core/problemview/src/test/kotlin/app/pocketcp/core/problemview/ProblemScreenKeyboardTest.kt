package app.pocketcp.core.problemview

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** At most this many Tab presses to reach a control; the view has far fewer stops before the first sample. */
private const val MAX_TABS = 30

/** Hardware keyboard on ChromeOS / DeX (QUALITY §9): Tab and Enter reach every control, PageDown/PageUp scroll. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class ProblemScreenKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    private val actions = RecordingActions()
    private val clipboard = FakeClipboard()
    private val listState = LazyListState()

    @Test
    fun tab_reachesRussian_andEnterSelectsIt() {
        show(shortStatement())
        rule.onNodeWithTag("problem_language_en").requestFocus()

        tabUntilFocused("problem_language_ru")
        rule.onNodeWithTag("problem_language_ru").performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle { assertEquals(listOf<Any>(StatementLanguage.Ru), actions.calls) }
    }

    @Test
    fun tabAndEnter_openTheReadingMenu() {
        show(shortStatement())
        rule.onNodeWithTag("problem_language_en").requestFocus()

        tabUntilFocused(READING_MENU_TAG)
        rule.onNodeWithTag(READING_MENU_TAG).performKeyInput { pressKey(Key.Enter) }

        rule.onNodeWithTag("problem_text_largest").assertExists()
        rule.onNodeWithTag("problem_width_narrow").assertExists()
    }

    @Test
    fun tab_reachesTheFirstSamplesCopy_andEnterCopiesIt() {
        show(shortStatement())
        rule.onNodeWithTag(READING_MENU_TAG).requestFocus()

        tabUntilFocused("sample_1_input_copy")
        rule.onNodeWithTag("sample_1_input_copy").performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle { assertEquals("2\n1 2\n", clipboard.copied?.text) }
    }

    @Test
    fun pageDown_scrollsForward_andPageUpBack() {
        show(shortStatement(paragraphs = 80))
        rule.onNodeWithTag("problem_language_en").requestFocus()
        assertEquals(0, listState.firstVisibleItemIndex)

        rule.onNodeWithTag("problem_language_en").performKeyInput { pressKey(Key.PageDown) }
        val afterDown = rule.runOnIdle { listState.firstVisibleItemIndex }
        assertTrue("PageDown should scroll, still at $afterDown", afterDown > 0)

        rule.onNodeWithTag("problem_language_en").performKeyInput { pressKey(Key.PageUp) }
        rule.runOnIdle { assertTrue(listState.firstVisibleItemIndex < afterDown) }
    }

    /** Shows [statement] loaded, in keyboard input mode, with [clipboard] as the clipboard. */
    private fun show(statement: Statement) {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            PocketCpTheme(mode = ThemeMode.Dark) {
                CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                    ProblemScreen(
                        state = ProblemUiState(Paco, ProblemStatus.Loaded, statement),
                        actions = actions,
                        onBack = {},
                        listState = listState,
                    )
                }
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
    }

    /** Presses Tab on the focused node until [tag] has focus; fails after [MAX_TABS] presses. */
    private fun tabUntilFocused(tag: String) {
        repeat(MAX_TABS) {
            if (hasFocus(tag)) return
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
            rule.waitForIdle()
        }
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    private fun hasFocus(tag: String): Boolean =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().any {
            it.config.getOrElse(SemanticsProperties.Focused) { false }
        }

    private class FakeClipboard : ClipboardManager {
        var copied: AnnotatedString? = null

        override fun getText(): AnnotatedString? = copied

        override fun setText(annotatedString: AnnotatedString) {
            copied = annotatedString
        }
    }
}
