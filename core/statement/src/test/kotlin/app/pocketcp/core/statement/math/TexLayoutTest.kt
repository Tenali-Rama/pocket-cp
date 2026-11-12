package app.pocketcp.core.statement.math

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Layout coverage for environments and commands that must keep their meaning when typeset. */
@Config(sdk = [SCREENSHOT_SDK])
@RunWith(AndroidJUnit4::class)
class TexLayoutTest {
    @Test
    fun plainMatrixHasNoDelimiters() {
        val glyphs = glyphs("\\left(\\begin{matrix}a & b \\\\ c & d\\end{matrix}\\right)")

        assertEquals("only the \\left( \\right) pair", 1, glyphs.count { it == "(" })
        assertEquals(1, glyphs.count { it == ")" })
    }

    @Test
    fun pmatrixAndBmatrixDrawTheirDelimiters() {
        assertTrue(glyphs("\\begin{pmatrix}a\\end{pmatrix}").containsAll(listOf("(", ")")))
        assertTrue(glyphs("\\begin{bmatrix}a\\end{bmatrix}").containsAll(listOf("[", "]")))
    }

    @Test
    fun casesLeftAlignsItsColumns() {
        val texts = texts("\\begin{cases}x & 1 \\\\ \\text{wide} & 2\\end{cases}")

        assertEquals(texts.first { it.value == "x" }.x, texts.first { it.value == "wide" }.x, 0.001f)
    }

    @Test
    fun rootIndexIsTypesetSmallAndRaised() {
        val fontSize = 24f
        val texts = texts("\\sqrt[3]{x}", fontSize)
        val index = texts.first { it.value == "3" }
        val body = texts.first { it.value == "x" }

        assertEquals(fontSize * ROOT_INDEX_FONT_SCALE, index.size, 0.001f)
        assertTrue("index sits above the body's baseline", index.baseline < body.baseline)
        assertTrue("index sits left of the body", index.x < body.x)
    }

    @Test
    fun unbalancedRootIndexFallsBackToItsSource() {
        val tex = "\\sqrt[3{x}"

        assertEquals(TexRenderResult.Fallback(tex), NativeTexTypesetter.typeset(tex, 24f, 0))
    }

    @Test
    fun mathbfIsDrawnBold() {
        assertEquals(SymbolStyle.Bold, texts("\\mathbf{v}").single().style)
    }

    private fun glyphs(tex: String): List<String> = texts(tex).map { it.value }

    private fun texts(
        tex: String,
        fontSize: Float = 24f,
    ): List<DrawCommand.Text> {
        val layout = (NativeTexTypesetter.typeset(tex, fontSize, 0xFFFFFFFF.toInt()) as TexRenderResult.Ready).layout
        return layout.commands.filterIsInstance<DrawCommand.Text>()
    }
}
