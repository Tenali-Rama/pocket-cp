package app.pocketcp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpSpacing
import app.pocketcp.core.designsystem.UiDensity
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Touch targets and hardware-keyboard activation of the rows and the banner's Retry, in both densities. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class RowsA11yTest {
    @get:Rule
    val rule = createComposeRule()

    private val rowTags =
        SampleContests.map { "contest_${it.id.value}" } + SampleProblems.map { "problem_${it.id.display}" }

    @Test
    fun comfortableRowsAreAtLeastTouchTargetTall() = assertRowsAtLeastTouchTargetTall(UiDensity.Comfortable)

    @Test
    fun compactRowsAreAtLeastTouchTargetTall() = assertRowsAtLeastTouchTargetTall(UiDensity.Compact)

    @Test
    fun enterKeyClicksEveryFocusedRowAndRetry() {
        val clicks = mutableMapOf<String, Int>()
        val inputModeManager =
            setContent(UiDensity.Comfortable) {
                Column {
                    SampleContests.forEach {
                        ContestRow(it, TestNow, onClick = { clicks.merge("contest_${it.id.value}", 1, Int::plus) })
                    }
                    SampleProblems.forEach {
                        val tag = "problem_${it.id.display}"
                        ProblemRow(it, false, false, false, onClick = { clicks.merge(tag, 1, Int::plus) })
                    }
                    RefreshErrorBanner(JudgeError.Offline(), onRetry = { clicks.merge("retry", 1, Int::plus) })
                }
            }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        rowTags.forEach { rule.onNodeWithTag(it).pressEnterWhenFocused() }
        rule.onNodeWithText("Retry").pressEnterWhenFocused()

        rule.runOnIdle { assertEquals((rowTags + "retry").associateWith { 1 }, clicks) }
    }

    private fun assertRowsAtLeastTouchTargetTall(density: UiDensity) {
        setContent(density) {
            // Scrollable, so rows below the small Robolectric window are measured at their natural height.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SampleContests.forEach { ContestRow(it, TestNow, onClick = {}) }
                SampleProblems.forEach {
                    ProblemRow(it, hideTags = true, hideRating = true, highlighted = false, onClick = {})
                }
            }
        }
        val min = PocketCpSpacing.of(density).touchTarget
        rowTags.forEach { rule.onNodeWithTag(it).assertHeightAtLeast(min) }
    }

    /** Sets [content] in the browse theme at [density]; returns the window's input mode manager. */
    private fun setContent(
        density: UiDensity,
        content: @Composable () -> Unit,
    ): InputModeManager {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            browseThemeWrapper(density)(true, content)
        }
        return inputModeManager
    }

    @OptIn(ExperimentalTestApi::class)
    private fun SemanticsNodeInteraction.pressEnterWhenFocused() {
        requestFocus()
        assertIsFocused()
        performKeyInput { pressKey(Key.Enter) }
    }

    private fun SemanticsNodeInteraction.assertHeightAtLeast(min: Dp) {
        val node = fetchSemanticsNode()
        with(node.layoutInfo.density) {
            val height = node.size.height.toDp()
            assertTrue("${node.config} height $height < $min", height >= min)
        }
    }
}
