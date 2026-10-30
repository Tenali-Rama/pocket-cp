package app.pocketcp.core.data

import app.pocketcp.core.database.ContestEntity
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MappersTest {
    @Test
    fun `contests round-trip through the entity, with and without a start`() {
        val round =
            Contest(
                ContestId(2000),
                "Codeforces Round 960",
                ContestPhase.Running,
                Instant.ofEpochSecond(1_721_000_000L),
                Duration.ofHours(2),
                false,
            )
        val gym = Contest(ContestId(100_001), "Training gym", ContestPhase.Finished, null, Duration.ofHours(5), true)

        listOf(round, gym).forEach { assertEquals(it, it.toEntity().toDomain()) }
        assertEquals(ContestEntity(2000, "Codeforces Round 960", "Running", 1_721_000_000L, 7_200L, false), round.toEntity())
    }

    @Test
    fun `every phase round-trips, and an unknown stored phase reads as Unknown`() {
        val base = ContestEntity(1, "Round 1", "Finished", null, 7_200L, false)

        ContestPhase.entries.forEach { phase -> assertEquals(phase, base.copy(phase = phase.name).toDomain().phase) }
        assertEquals(ContestPhase.Unknown, base.copy(phase = "Paused").toDomain().phase)
    }

    @Test
    fun `problems round-trip through the entity, with and without optional fields`() {
        val rated = Problem(ProblemId(1234, "B1"), "Easy version", 1600, listOf("dp", "greedy"), 4_321)
        val unrated = Problem(ProblemId(4, "A"), "Watermelon", null, emptyList(), null)

        listOf(rated, unrated).forEach { assertEquals(it, it.toEntity().toDomain()) }
    }
}
