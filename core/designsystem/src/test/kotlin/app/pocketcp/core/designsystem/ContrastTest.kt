package app.pocketcp.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** WCAG AA (spec §16): normal-size text needs a contrast ratio of at least 4.5:1. */
class ContrastTest {
    @Test
    fun contrastRatio_matchesWcagReferenceValues() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.Red, Color.Red), 0.01)
        // #767676 on white is the classic "just passes AA" grey.
        assertEquals(4.54, contrastRatio(Color(0xFF767676), Color.White), 0.01)
    }

    @Test
    fun everyTextTokenOnItsBackground_reachesAa_inEveryTheme() {
        ThemeMode.entries.forEach { mode ->
            val c = PocketCpColors.of(mode)
            val pairs =
                listOf(
                    "onBackground/background" to (c.onBackground to c.background),
                    "onSurface/surface" to (c.onSurface to c.surface),
                    "onSurface/surfaceRaised" to (c.onSurface to c.surfaceRaised),
                    "onSurfaceMuted/background" to (c.onSurfaceMuted to c.background),
                    "onSurfaceMuted/surface" to (c.onSurfaceMuted to c.surface),
                    "onSurfaceMuted/surfaceRaised" to (c.onSurfaceMuted to c.surfaceRaised),
                    "onPrimary/primary" to (c.onPrimary to c.primary),
                    "primary/surface" to (c.primary to c.surface),
                    "error/background" to (c.error to c.background),
                    "error/surface" to (c.error to c.surface),
                    "error/surfaceRaised" to (c.error to c.surfaceRaised),
                    "onError/error" to (c.onError to c.error),
                )
            pairs.forEach { (name, pair) -> assertAa("$mode $name", pair.first, pair.second) }
        }
    }

    @Test
    fun everyRankColourOnTheSurfaces_reachesAa_inEveryTheme() {
        ThemeMode.entries.forEach { mode ->
            val c = PocketCpColors.of(mode)
            Rank.entries.forEach { rank ->
                val colour = c.rank.of(rank)
                assertAa("$mode $rank/surface", colour, c.surface)
                assertAa("$mode $rank/background", colour, c.background)
                assertAa("$mode $rank/surfaceRaised", colour, c.surfaceRaised)
            }
        }
    }

    @Test
    fun amoledBackgroundIsPureBlack() {
        val amoled = PocketCpColors.of(ThemeMode.Amoled)
        assertEquals(Color.Black, amoled.background)
        assertEquals(Color.Black, amoled.surface)
    }

    @Test
    fun assertAa_rejectsALowContrastPair() {
        val failed = runCatching { assertAa("grey on grey", Color(0xFF777777), Color(0xFF888888)) }.isFailure
        assertTrue("4.5:1 check must fail for a low-contrast pair", failed)
    }

    private fun assertAa(
        name: String,
        foreground: Color,
        background: Color,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name: contrast %.2f:1 < $AA_NORMAL_TEXT:1".format(ratio), ratio >= AA_NORMAL_TEXT)
    }

    private fun contrastRatio(
        a: Color,
        b: Color,
    ): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private companion object {
        const val AA_NORMAL_TEXT = 4.5
    }
}
