package app.pocketcp.core.data

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import app.pocketcp.core.database.PocketCpDatabase
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/** `ContestRepository.activeContests`: the bounded view over the regular contests' cache and refresh. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActiveContestsTest {
    private lateinit var database: PocketCpDatabase
    private val source = FakeJudgeSource()

    @Before
    fun open() {
        database = inMemoryDatabase()
    }

    @After
    fun close() = database.close()

    @Test
    fun `first item has no data and refreshes, then only running and the next upcoming are emitted`() =
        runTest {
            val response = CompletableDeferred<List<Contest>>()
            source.onContests = { response.await() }

            contests().activeContests(2).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(null, null, refreshing = true, error = null), awaitItem())

                response.complete(listOf(finished, third, running, first, second, gym))

                val loaded = awaitMatching { it.data != null && !it.refreshing }
                assertEquals(Cached(listOf(running, first, second), Instant.EPOCH, false, null), loaded)
            }
        }

    @Test
    fun `an upcoming contest that starts moves to running and the next one takes its slot`() =
        runTest {
            cache(finished, running, first, second, third)
            val repository = contests()
            val started = first.copy(phase = ContestPhase.Running)
            source.onContests = { listOf(finished, running, started, second, third) }

            repository.activeContests(2).test(timeout = FLOW_TIMEOUT) {
                assertEquals(listOf(running, first, second), awaitItem().data)

                repository.refresh(gym = false, force = true)

                val expected = listOf(running, started, second, third)
                assertEquals(expected, awaitMatching { it.data == expected && !it.refreshing }.data)
            }
        }

    @Test
    fun `a negative upcoming limit is rejected`() =
        runTest {
            assertThrows(IllegalArgumentException::class.java) { contests().activeContests(-1) }
        }

    @Test
    fun `the bounded view and the full list share one fetch`() =
        runTest {
            val repository = contests()
            val response = CompletableDeferred<List<Contest>>()
            source.onContests = { response.await() }

            turbineScope {
                val active = repository.activeContests(5).testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                val all = repository.contests(gym = false).testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                assertTrue(active.awaitItem().refreshing)
                assertTrue(all.awaitItem().refreshing)

                response.complete(listOf(finished, running, first))

                active.awaitMatching { it.data == listOf(running, first) && !it.refreshing }
                all.awaitMatching { it.data != null && !it.refreshing }
                active.cancel()
                all.cancel()
            }
            assertEquals(1, source.contestCalls)
        }

    @Test
    fun `a failed refresh keeps the bounded data and shows the error`() =
        runTest {
            cache(finished, running, first)
            advanceTimeBy(6.minutes)
            val rateLimited = JudgeError.RateLimited()
            val answer = CompletableDeferred<Unit>()
            source.onContests = {
                answer.await()
                throw rateLimited
            }

            contests().activeContests(5).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(listOf(running, first), Instant.EPOCH, true, null), awaitItem())

                answer.complete(Unit)

                val failed = awaitItem()
                assertEquals(listOf(running, first), failed.data)
                assertFalse(failed.refreshing)
                assertEquals(rateLimited, failed.error)
            }
        }

    @Test
    fun `a fresh cache is emitted without a fetch`() =
        runTest {
            cache(finished, running, first)
            advanceTimeBy(4.minutes)

            contests().activeContests(5).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(listOf(running, first), Instant.EPOCH, false, null), awaitItem())
                runCurrent()
                assertEquals(0, source.contestCalls)
            }
        }

    private suspend fun TestScope.cache(vararg contests: Contest) =
        database.contestDao().replaceAll(false, contests.map { it.toEntity() }, fetchedAtEpochMillis = currentTime)

    private fun TestScope.contests() =
        CachedContestRepository(
            source,
            database.contestDao(),
            database.cacheMetaDao(),
            SchedulerClock(testScheduler),
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )

    private companion object {
        fun regular(
            id: Int,
            phase: ContestPhase,
            startSeconds: Long,
        ) = Contest(ContestId(id), "Round $id", phase, Instant.ofEpochSecond(startSeconds), Duration.ofHours(2), false)

        val finished = regular(1, ContestPhase.Finished, 1_700_000_000L)
        val running = regular(2, ContestPhase.Running, 1_800_000_000L)
        val first = regular(3, ContestPhase.Upcoming, 1_800_100_000L)
        val second = regular(4, ContestPhase.Upcoming, 1_800_200_000L)
        val third = regular(5, ContestPhase.Upcoming, 1_800_300_000L)
        val gym = regular(100_001, ContestPhase.Running, 1L).copy(isGym = true)
    }
}
