package app.pocketcp.core.model

import java.time.Duration
import java.time.Instant

/** A contest id on its judge. */
@JvmInline
value class ContestId(
    val value: Int,
)

/** Where a contest is in its lifecycle. [Unknown] covers judge phases the domain doesn't model. */
enum class ContestPhase { Upcoming, Running, Finished, Unknown }

/** A contest's scoring rules. [Unknown] covers rules the domain doesn't model. */
enum class ContestKind { Cf, Icpc, Ioi, Unknown }

/**
 * The audience a contest is for, read from its name. [Educational] and [Global] win over a division the name also
 * states ("Educational Codeforces Round 170 (Rated for Div. 2)" is [Educational]). [Other] is a name without one.
 */
enum class ContestDivision {
    Div1,
    Div2,
    Div3,
    Div4,
    Div1And2,
    Educational,
    Global,
    Other,
    ;

    companion object {
        private val educational = Regex("""\bEducational\b""", RegexOption.IGNORE_CASE)
        private val global = Regex("""\bGlobal Round\b""", RegexOption.IGNORE_CASE)
        private val combined = Regex("""\bDiv\.?\s*1\s*\+\s*(Div\.?\s*)?2\b""", RegexOption.IGNORE_CASE)
        private val single = Regex("""\bDiv\.?\s*([1-4])\b""", RegexOption.IGNORE_CASE)

        /** The division a contest [name] states, or [Other]. */
        fun of(name: String): ContestDivision =
            when {
                educational.containsMatchIn(name) -> Educational
                global.containsMatchIn(name) -> Global
                combined.containsMatchIn(name) -> Div1And2
                else ->
                    when (single.find(name)?.groupValues?.get(1)) {
                        "1" -> Div1
                        "2" -> Div2
                        "3" -> Div3
                        "4" -> Div4
                        else -> Other
                    }
            }
    }
}

/**
 * A contest or gym, as the UI and cache see it. [start] is `null` when the judge doesn't publish one.
 * [division] is derived from [name] and never stored.
 */
data class Contest(
    val id: ContestId,
    val name: String,
    val phase: ContestPhase,
    val start: Instant?,
    val duration: Duration,
    val isGym: Boolean,
    val kind: ContestKind = ContestKind.Unknown,
) {
    val division: ContestDivision = ContestDivision.of(name)
}
