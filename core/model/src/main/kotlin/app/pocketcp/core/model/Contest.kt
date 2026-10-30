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

/** A contest or gym, as the UI and cache see it. [start] is `null` when the judge doesn't publish one. */
data class Contest(
    val id: ContestId,
    val name: String,
    val phase: ContestPhase,
    val start: Instant?,
    val duration: Duration,
    val isGym: Boolean,
)
