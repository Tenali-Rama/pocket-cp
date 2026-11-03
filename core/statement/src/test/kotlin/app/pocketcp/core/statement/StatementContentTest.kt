package app.pocketcp.core.statement

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getFirstLinkBounds
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** User-facing behaviour of the native statement renderer. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [app.pocketcp.core.screenshot.SCREENSHOT_SDK])
class StatementContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun copyingInputPreservesEveryCharacterAndNewline() {
        val input = "first line\nsecond line\n"
        setContent(statement(samples = listOf(Sample(input, "answer\n"))))

        rule.onNodeWithTag("sample_1_input_copy").performClick()

        val clipboard = ApplicationProvider.getApplicationContext<Context>().clipboard()
        assertEquals(
            input,
            clipboard.primaryClip
                ?.getItemAt(0)
                ?.text
                ?.toString(),
        )
    }

    @Test
    fun tappingAParagraphLinkPassesItsUrlToTheCaller() {
        var opened: String? = null
        setContent(
            statement(
                sections =
                    listOf(
                        StatementSection(
                            SectionKind.Legend,
                            "Legend",
                            listOf(StatementBlock.Paragraph(listOf(Inline.Link("editorial", "https://codeforces.com/blog/1")))),
                        ),
                    ),
            ),
        ) { opened = it }

        rule.onNodeWithText("editorial").performFirstLinkClick()

        assertEquals("https://codeforces.com/blog/1", opened)
    }

    @Test
    fun tappingParagraphTextOutsideLinkDoesNotOpenItsUrl() {
        var opened: String? = null
        setContent(
            statement(
                sections =
                    listOf(
                        StatementSection(
                            SectionKind.Legend,
                            "Legend",
                            listOf(
                                StatementBlock.Paragraph(
                                    listOf(
                                        Inline.Text("prefix "),
                                        Inline.Link("editorial", "https://codeforces.com/blog/1"),
                                    ),
                                ),
                            ),
                        ),
                    ),
            ),
        ) { opened = it }

        val paragraph = rule.onNodeWithText("prefix editorial")
        val linkBounds = paragraph.getFirstLinkBounds()
        paragraph.performTouchInput { click(Offset(1f, linkBounds!!.center.y)) }

        assertNull(opened)
    }

    @Test
    fun sectionTitlesAreHeadings() {
        setContent(
            statement(
                sections =
                    listOf(
                        StatementSection(SectionKind.Input, "Input", listOf(StatementBlock.Paragraph(listOf(Inline.Text("x"))))),
                    ),
            ),
        )

        rule.onNodeWithText("Input").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun sampleCopyButtonsMeetTheDesignSystemTouchTarget() {
        setContent(statement(samples = listOf(Sample("1", "2"))))

        rule.onNodeWithTag("sample_1_input_copy").assertTouchTargetAtLeast(48.dp)
        rule.onNodeWithTag("sample_1_output_copy").assertTouchTargetAtLeast(48.dp)
    }

    private fun setContent(
        statement: Statement,
        onOpenLink: (String) -> Unit = {},
    ) {
        rule.setContent {
            PocketCpTheme {
                StatementContent(
                    statement,
                    textScale = 1f,
                    maxWidth = Dp.Infinity,
                    onOpenLink = onOpenLink,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun Context.clipboard(): ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun statement(
        sections: List<StatementSection> = emptyList(),
        samples: List<Sample> = emptyList(),
    ) = Statement(
        problem = ProblemId(1, "A"),
        language = StatementLanguage.En,
        title = "A. Test statement",
        timeLimitMillis = 2_000,
        memoryLimitMegabytes = 256,
        inputFile = null,
        outputFile = null,
        sections = sections,
        samples = samples,
        materials = listOf(MaterialLink("Editorial", "https://codeforces.com/blog/1", MaterialKind.Tutorial)),
    )

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTouchTargetAtLeast(minimum: Dp) {
        val node = fetchSemanticsNode()
        with(node.layoutInfo.density) {
            assertTrue(
                "touch height ${node.touchBoundsInRoot.height.toDp()} < $minimum",
                node.touchBoundsInRoot.height.toDp() >= minimum,
            )
            assertTrue(
                "touch width ${node.touchBoundsInRoot.width.toDp()} < $minimum",
                node.touchBoundsInRoot.width.toDp() >= minimum,
            )
        }
    }
}
