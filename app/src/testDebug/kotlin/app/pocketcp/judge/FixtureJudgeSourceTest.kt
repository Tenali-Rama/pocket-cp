package app.pocketcp.judge

import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.testing.Fixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FixtureJudgeSourceTest {
    @get:Rule
    val emptyAssets = TemporaryFolder()

    private val now = Instant.parse("2026-10-31T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val recorded = File(Fixtures.root, "codeforces/api")

    @Test
    fun `contests are shifted so the recording moment is now`() =
        runTest {
            val contests = withSource(recorded) { it.contests(gym = false) }

            // contest.list.json: 2278 is BEFORE at relativeTimeSeconds -147320; 2272 is FINISHED at 284680.
            val upcoming = contests.first { it.phase == ContestPhase.Upcoming }
            assertEquals(2278, upcoming.id.value)
            assertEquals(now.plusSeconds(147_320), upcoming.start)
            val finished = contests.single { it.id.value == 2272 }
            assertEquals(now.minusSeconds(284_680), finished.start)
            assertEquals(ContestPhase.Finished, finished.phase)
        }

    @Test
    fun `gyms move by the same shift and keep a missing start`() =
        runTest {
            val gyms = withSource(recorded) { it.contests(gym = true) }

            // contest.list.json was recorded at 1793309980; gym 100299 starts at 1507293000.
            val shift = now.epochSecond - 1_793_309_980
            assertTrue(gyms.all { it.isGym })
            assertEquals(Instant.ofEpochSecond(1_507_293_000 + shift), gyms.single { it.id.value == 100299 }.start)
            assertNull(gyms.single { it.id.value == 100001 }.start)
        }

    @Test
    fun `the problemset is the recorded one`() =
        runTest {
            val problems = withSource(recorded) { it.problemset() }

            // problemset.problems.json holds 3 problems, all with a contestId.
            assertEquals(3, problems.size)
        }

    @Test
    fun `a missing asset is a malformed response`() =
        runTest {
            val failure = withSource(emptyAssets.root) { runCatching { it.problemset() }.exceptionOrNull() }

            assertTrue("got $failure", failure is JudgeError.Malformed)
        }

    /** Runs [block] on a [FixtureJudgeSource] reading [dir], with its queue on the test scheduler. */
    private suspend fun <T> TestScope.withSource(
        dir: File,
        block: suspend (JudgeSource) -> T,
    ): T {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            return block(FixtureJudgeSource({ name -> File(dir, name).readText() }, clock, scope, dispatcher))
        } finally {
            scope.cancel()
        }
    }
}
