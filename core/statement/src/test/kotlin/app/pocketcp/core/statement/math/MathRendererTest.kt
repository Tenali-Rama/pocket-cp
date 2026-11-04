package app.pocketcp.core.statement.math

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
