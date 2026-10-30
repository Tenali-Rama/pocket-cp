package app.pocketcp.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `PocketCpTheme(density, textScale, codeTextScale)`: what each input changes, and what it leaves alone. */
class ThemeScaleTest {
    private val base = pocketCpTypography(FontFamily.Monospace, codeLigatures = true)

    @Test
    fun textScaleMultipliesEveryUiStyleButNotCode() {
        val scaled = pocketCpTypography(FontFamily.Monospace, codeLigatures = true, textScale = 1.3f)

        base.uiStyles().zip(scaled.uiStyles()).forEach { (before, after) ->
            assertEquals(before.fontSize.value * 1.3f, after.fontSize.value, DELTA)
            assertEquals(before.lineHeight.value * 1.3f, after.lineHeight.value, DELTA)
        }
        assertEquals(base.code, scaled.code)
        assertEquals(base.codeSmall, scaled.codeSmall)
    }

    @Test
    fun codeTextScaleMultipliesOnlyCodeStyles() {
        val scaled = pocketCpTypography(FontFamily.Monospace, codeLigatures = true, codeTextScale = 1.5f)

        assertEquals(base.code.fontSize.value * 1.5f, scaled.code.fontSize.value, DELTA)
        assertEquals(base.codeSmall.lineHeight.value * 1.5f, scaled.codeSmall.lineHeight.value, DELTA)
        assertEquals(base.uiStyles(), scaled.uiStyles())
    }

    @Test
    fun defaultScalesKeepTheOriginalTypeScale() {
        assertEquals(16f, base.body.fontSize.value)
        assertEquals(24f, base.body.lineHeight.value)
        assertEquals(14f, base.code.fontSize.value)
    }

    @Test
    fun compactTrimsRowAndCardPaddingButKeepsTouchTargets() {
        val comfortable = PocketCpSpacing.of(UiDensity.Comfortable)
        val compact = PocketCpSpacing.of(UiDensity.Compact)

        assertEquals(PocketCpSpacing(), comfortable)
        assertTrue(compact.listItemVertical < comfortable.listItemVertical)
        assertTrue(compact.listItemTwoLineHeight < comfortable.listItemTwoLineHeight)
        assertTrue(compact.cardVertical < comfortable.cardVertical)
        listOf(compact.listItemHeight, compact.listItemTwoLineHeight).forEach {
            assertTrue("$it < ${compact.touchTarget}", it >= compact.touchTarget)
        }
        val compactWithComfortableRows =
            compact.copy(
                listItemVertical = comfortable.listItemVertical,
                listItemHeight = comfortable.listItemHeight,
                listItemTwoLineHeight = comfortable.listItemTwoLineHeight,
                cardVertical = comfortable.cardVertical,
            )
        assertEquals("only row and card tokens follow density", comfortable, compactWithComfortableRows)
    }

    private fun PocketCpTypography.uiStyles(): List<TextStyle> = listOf(display, headline, title, body, bodySmall, label)

    private companion object {
        const val DELTA = 0.001f
    }
}
