package app.pocketcp.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A Codeforces rank. [minRating] is the lowest rating of the rank (inclusive); [Unrated] has none.
 * Declared from the lowest to the highest rated rank, then [Unrated].
 */
@Suppress("MagicNumber") // The rating boundaries of Codeforces ranks.
enum class Rank(
    val minRating: Int?,
) {
    Newbie(Int.MIN_VALUE),
    Pupil(1200),
    Specialist(1400),
    Expert(1600),
    CandidateMaster(1900),
    Master(2100),
    InternationalMaster(2300),
    Grandmaster(2400),
    InternationalGrandmaster(2600),
    LegendaryGrandmaster(3000),
    Unrated(null),
    ;

    companion object {
        private val rated = entries.filter { it.minRating != null }

        /** The rank for [rating]; `null` (no rated contest yet) is [Unrated]. */
        fun ofRating(rating: Int?): Rank {
            if (rating == null) return Unrated
            return rated.last { (it.minRating ?: Int.MIN_VALUE) <= rating }
        }
    }
}

/**
 * The colour of each rank in one theme. Hues follow Codeforces; each theme has its own shades so that every
 * rank colour reaches WCAG AA (4.5:1) as text on that theme's surfaces (docs/decisions/0006-theme-model.md).
 */
@Immutable
data class RankColors(
    val newbie: Color,
    val pupil: Color,
    val specialist: Color,
    val expert: Color,
    val candidateMaster: Color,
    val master: Color,
    val grandmaster: Color,
    val unrated: Color,
) {
    /** Codeforces shares a colour between Master and International Master, and among the three grandmaster ranks. */
    fun of(rank: Rank): Color =
        when (rank) {
            Rank.Newbie -> newbie
            Rank.Pupil -> pupil
            Rank.Specialist -> specialist
            Rank.Expert -> expert
            Rank.CandidateMaster -> candidateMaster
            Rank.Master, Rank.InternationalMaster -> master
            Rank.Grandmaster, Rank.InternationalGrandmaster, Rank.LegendaryGrandmaster -> grandmaster
            Rank.Unrated -> unrated
        }

    /** Shorthand for `of(Rank.ofRating(rating))`. */
    fun ofRating(rating: Int?): Color = of(Rank.ofRating(rating))
}
