package app.pocketcp.core.statement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, content = content)
}

/** Native math captures: inline baseline flow, display limits/fractions/cases and invalid source fallback. */
class StatementMathScreenshotTest : ScreenshotTest() {
    @Test
    fun statementMath() = captureMatrix("statementMath", PocketCpThemeWrapper) { MathGallery() }

    @Test
    fun statementMathTextLargest() =
        captureSingle("statementMath_textLargest", ScreenshotConfig(), PocketCpThemeWrapper) {
            MathGallery(textScale = 1.3f)
        }

    @Test
    fun statementMathInvalid() =
        captureSingle("statementMath_invalid", ScreenshotConfig(), PocketCpThemeWrapper) {
            InvalidMathGallery()
        }
}

@Composable
private fun MathGallery(textScale: Float = 1f) {
    Box(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        StatementContent(
            statement = mathStatement(),
            textScale = textScale,
            maxWidth = Dp.Infinity,
            onOpenLink = {},
        )
    }
}

@Composable
private fun InvalidMathGallery() {
    Box(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        StatementContent(
            statement =
                mathStatement().copy(
                    sections =
                        listOf(
                            StatementSection(
                                SectionKind.Legend,
                                "Invalid formula",
                                listOf(StatementBlock.Paragraph(listOf(Inline.Math("\\frac{")))),
                            ),
                        ),
                    materials = emptyList(),
                ),
            textScale = 1f,
            maxWidth = Dp.Infinity,
            onOpenLink = {},
        )
    }
}

private fun mathStatement() =
    Statement(
        problem = ProblemId(1900, "A"),
        language = StatementLanguage.En,
        title = "A. Native math",
        timeLimitMillis = 2_000,
        memoryLimitMegabytes = 256,
        inputFile = null,
        outputFile = null,
        sections =
            listOf(
                StatementSection(
                    SectionKind.Legend,
                    "Legend",
                    listOf(
                        StatementBlock.Paragraph(
                            listOf(
                                Inline.Text("For every "),
                                Inline.Math("x^2 + y^2"),
                                Inline.Text(" the following identity holds."),
                            ),
                        ),
                        StatementBlock.DisplayMath(
                            "\\sum_{i=1}^{n} \\frac{i}{2} = \\begin{cases} n(n+1)/4 & n \\ge 1 \\\\ 0 & \\text{otherwise} \\end{cases}",
                        ),
                    ),
                ),
            ),
        samples = emptyList(),
        materials = listOf(MaterialLink("Editorial", "https://codeforces.com/blog/1", MaterialKind.Tutorial)),
    )
