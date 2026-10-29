package app.pocketcp.core.designsystem.component

import app.pocketcp.core.designsystem.PocketCpColors
import app.pocketcp.core.designsystem.Rank
import app.pocketcp.core.designsystem.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RankedHandleTest {
    private val colors = PocketCpColors.of(ThemeMode.Light)

    @Test
    fun legendaryGrandmasterFirstLetterUsesContrastColour() {
        val text = rankedHandleText("legend", Rank.LegendaryGrandmaster, colors)

        assertEquals("legend", text.text)
        val (first, rest) = text.spanStyles
        assertEquals(colors.onBackground, first.item.color)
        assertEquals(0 to 1, first.start to first.end)
        assertEquals(colors.rank.grandmaster, rest.item.color)
        assertEquals(1 to 6, rest.start to rest.end)
    }

    @Test
    fun otherRanksAreOneColour() {
        val ranks = listOf(Rank.Newbie, Rank.Expert, Rank.Grandmaster, Rank.InternationalGrandmaster, Rank.Unrated)
        ranks.forEach { rank ->
            val spans = rankedHandleText("handle", rank, colors).spanStyles

            assertEquals(rank.name, 1, spans.size)
            assertEquals(rank.name, colors.rank.of(rank), spans.single().item.color)
        }
    }

    @Test
    fun emptyLegendaryGrandmasterHandleDoesNotCrash() {
        assertEquals("", rankedHandleText("", Rank.LegendaryGrandmaster, colors).text)
    }

    @Test
    fun oneLetterLegendaryGrandmasterHandleIsAllContrastColour() {
        val first = rankedHandleText("x", Rank.LegendaryGrandmaster, colors).spanStyles.first()

        assertEquals(colors.onBackground, first.item.color)
        assertEquals(0 to 1, first.start to first.end)
    }
}
