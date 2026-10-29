package app.pocketcp.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RankColorsTest {
    /** Each Codeforces rank boundary as (last rating below it, its rank) and (first rating at it, its rank). */
    private val boundaries =
        listOf(
            1199 to Rank.Newbie,
            1200 to Rank.Pupil,
            1399 to Rank.Pupil,
            1400 to Rank.Specialist,
            1599 to Rank.Specialist,
            1600 to Rank.Expert,
            1899 to Rank.Expert,
            1900 to Rank.CandidateMaster,
            2099 to Rank.CandidateMaster,
            2100 to Rank.Master,
            2299 to Rank.Master,
            2300 to Rank.InternationalMaster,
            2399 to Rank.InternationalMaster,
            2400 to Rank.Grandmaster,
            2599 to Rank.Grandmaster,
            2600 to Rank.InternationalGrandmaster,
            2999 to Rank.InternationalGrandmaster,
            3000 to Rank.LegendaryGrandmaster,
        )

    @Test
    fun ofRating_mapsEveryBoundaryToItsRank() {
        boundaries.forEach { (rating, rank) -> assertEquals("rating $rating", rank, Rank.ofRating(rating)) }
    }

    @Test
    fun ofRating_extremesAndUnrated() {
        assertEquals(Rank.Newbie, Rank.ofRating(0))
        assertEquals(Rank.Newbie, Rank.ofRating(-100))
        assertEquals(Rank.LegendaryGrandmaster, Rank.ofRating(4000))
        assertEquals(Rank.Unrated, Rank.ofRating(null))
    }

    @Test
    fun ofRating_givesTheRankColourAtEveryBoundary_inEveryTheme() {
        ThemeMode.entries.forEach { mode ->
            val rank = PocketCpColors.of(mode).rank
            val expected =
                mapOf(
                    Rank.Newbie to rank.newbie,
                    Rank.Pupil to rank.pupil,
                    Rank.Specialist to rank.specialist,
                    Rank.Expert to rank.expert,
                    Rank.CandidateMaster to rank.candidateMaster,
                    Rank.Master to rank.master,
                    Rank.InternationalMaster to rank.master,
                    Rank.Grandmaster to rank.grandmaster,
                    Rank.InternationalGrandmaster to rank.grandmaster,
                    Rank.LegendaryGrandmaster to rank.grandmaster,
                )
            boundaries.forEach { (rating, r) ->
                assertEquals("$mode rating $rating", expected.getValue(r), rank.ofRating(rating))
            }
            assertEquals("$mode unrated", rank.unrated, rank.ofRating(null))
        }
    }

    @Test
    fun colourChangesAcrossEveryBoundaryThatChangesColourOnCodeforces() {
        // Master/International Master and the three grandmaster ranks share a colour on Codeforces.
        val colourChanges = listOf(1200, 1400, 1600, 1900, 2100, 2400)
        ThemeMode.entries.forEach { mode ->
            val rank = PocketCpColors.of(mode).rank
            colourChanges.forEach { rating ->
                assertNotEquals("$mode at $rating", rank.ofRating(rating - 1), rank.ofRating(rating))
            }
            assertNotEquals("$mode unrated vs newbie", rank.newbie, rank.unrated)
        }
    }

    @Test
    fun rankColoursAreDistinctPerTheme() {
        ThemeMode.entries.forEach { mode ->
            val rank = PocketCpColors.of(mode).rank
            val colours = Rank.entries.map(rank::of).distinct()
            // 11 ranks, 8 colours: Master = International Master, Grandmaster = IGM = LGM.
            assertEquals("$mode", 8, colours.size)
        }
    }
}
