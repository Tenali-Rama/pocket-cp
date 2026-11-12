package app.pocketcp.core.statement.math

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Parser, fallback, cache and accessibility coverage for the native TeX renderer. */
@Config(sdk = [SCREENSHOT_SDK])
@RunWith(AndroidJUnit4::class)
class MathRendererTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun commonCodeforcesTexIsTypesetNatively() {
        val formulas =
            listOf(
                "\\frac{a+b}{c}",
                "\\sum_{i=1}^{n} i^2 + \\prod_{j=1}^{m} j",
                "x \\le y \\ne z \\cdot \\ldots \\dots \\lfloor x \\rfloor",
                "\\binom{n}{k} + \\sqrt{x+1} + \\text{answer} + \\mathcal{F} \\oplus y",
                "\\begin{matrix}a & b \\\\ c & d\\end{matrix}",
                "\\begin{cases}x & x > 0 \\\\ 0 & \\text{otherwise}\\end{cases}",
            )

        formulas.forEach { formula ->
            val result = NativeTexTypesetter.typeset(formula, 24f, 0xFFFFFFFF.toInt())
            assertTrue("$formula should be accepted", result is TexRenderResult.Ready)
            assertTrue((result as TexRenderResult.Ready).layout.width > 0f)
        }
    }

    @Test
    fun invalidTexFallsBackToItsSource() {
        val unbalanced = "\\frac{"
        val unknown = "\\unknown{x}"
        rule.setContent {
            PocketCpTheme {
                LocalMathRenderer.current.Inline(unbalanced, 1f)
                LocalMathRenderer.current.Inline(unknown, 1f)
            }
        }

        rule.onNodeWithText(unbalanced).assertExists()
        rule.onNodeWithText(unknown).assertExists()
    }

    @Test
    fun sameFormulaAtSameSizeUsesTheTypesetCache() {
        NativeTexTypesetter.clearForTests()
        val formula = "\\frac{x^2}{y}"

        NativeTexTypesetter.typeset(formula, 28f, 0xFFFFFFFF.toInt())
        NativeTexTypesetter.typeset(formula, 28f, 0xFFFFFFFF.toInt())

        assertEquals(1, NativeTexTypesetter.typesetCountForTests())
    }

    @Test
    fun inlineMetricsPutTheFormulaBaselineOnTheTextBaseline() {
        val textCenter = 5f
        val metrics = InlineMathMetrics.of(width = 40.2f, ascent = 30f, descent = 12f, textCenter = textCenter)

        // TextCenter alignment puts the placeholder's centre `textCenter` above the text baseline.
        val textBaselineFromTop = metrics.height / 2 + textCenter
        assertEquals(textBaselineFromTop, metrics.baseline, 0.001f)
        assertTrue("ascent fits above", metrics.baseline >= 30f)
        assertTrue("descent fits below", metrics.height - metrics.baseline >= 12f)
        assertEquals(41f, metrics.width, 0.001f)
    }

    @Test
    fun fractionBarSitsOnTheMathAxisNotTheBaseline() {
        val fontSize = 24f
        val result = NativeTexTypesetter.typeset("\\frac{n}{2}", fontSize, 0xFFFFFFFF.toInt())
        val layout = (result as TexRenderResult.Ready).layout

        // The denominator hangs below the text baseline instead of resting on it.
        assertTrue(layout.height - layout.baseline > fontSize * 0.4f)
    }

    @Test
    fun inlinePlaceholderIsSizedFromTheTypesetFormula() {
        val placeholders = mutableMapOf<String, Placeholder>()
        val letter = "x"
        val sum = "x^2 + y^2 + z^2"
        val fraction = "\\frac{n}{2}"
        rule.setContent {
            PocketCpTheme {
                listOf(letter, sum, fraction).forEach {
                    placeholders[it] = LocalMathRenderer.current.inlinePlaceholder(it, 1f)
                }
            }
        }
        rule.waitForIdle()

        val short = placeholders.getValue(letter)
        assertTrue("wider formula, wider room", placeholders.getValue(sum).width.value > short.width.value * 3)
        val fractionHeight = placeholders.getValue(fraction).height.value
        assertTrue("fraction is taller than a letter", fractionHeight > short.height.value)
        assertEquals(PlaceholderVerticalAlign.TextCenter, short.placeholderVerticalAlign)
    }

    @Test
    fun contentDescriptionIsTheOriginalTexSource() {
        val formula = "\\sqrt{x}"
        rule.setContent {
            PocketCpTheme {
                LocalMathRenderer.current.Display(formula, 1f)
            }
        }

        rule.onNodeWithContentDescription(formula).assertExists()
    }
}
