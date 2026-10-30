package app.pocketcp.core.data

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import app.pocketcp.core.database.PocketCpDatabase
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Repositories on an in-memory Room cache, a fake judge gated by the test, and virtual time as the clock. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StaleWhileRevalidateTest {
    private lateinit var database: PocketCpDatabase
    private val source = FakeJudgeSource()

    @Before
    fun open() {
        database = inMemoryDatabase()
    }

    @After
    fun close() = database.close()

    @Test
    fun `fresh cache is emitted without calling the source`() =
        runTest {
            database.problemDao().replaceAll(listOf(watermelon.toEntity()), currentTime)
            advanceTimeBy(59.minutes)

            problems().problemset().test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(listOf(watermelon), Instant.EPOCH, refreshing = false, error = null), awaitItem())
                runCurrent()
                assertEquals(0, source.problemsetCalls)
            }
        }

    @Test
    fun `stale cache is emitted as refreshing, then replaced by the new data`() =
        runTest {
            database.contestDao().replaceAll(gym = false, contests = listOf(oldRound.toEntity()), fetchedAtEpochMillis = currentTime)
            advanceTimeBy(6.minutes)
            val response = CompletableDeferred<List<Contest>>()
            source.onContests = { response.await() }

            contests().contests(gym = false).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(listOf(oldRound), Instant.EPOCH, refreshing = true, error = null), awaitItem())

                response.complete(listOf(newRound, oldRound))

                val refreshed = awaitMatching { it.data == listOf(newRound, oldRound) && !it.refreshing }
                assertEquals(Cached(listOf(newRound, oldRound), Instant.ofEpochMilli(currentTime), false, null), refreshed)
                assertEquals(1, source.contestCalls)
            }
        }

    @Test
    fun `empty cache and Offline give no data and the error`() =
        runTest {
            val offline = JudgeError.Offline()
            val answer = CompletableDeferred<Unit>()
            source.onProblemset = {
                answer.await()
                throw offline
            }

            problems().problemset().test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(null, null, refreshing = true, error = null), awaitItem())

                answer.complete(Unit)

                assertEquals(Cached(null, null, refreshing = false, error = offline), awaitItem())
            }
        }

    @Test
    fun `cached data is kept when the refresh is RateLimited`() =
        runTest {
            database.problemDao().replaceAll(listOf(watermelon.toEntity()), currentTime)
            advanceTimeBy(2.hours)
            val rateLimited = JudgeError.RateLimited()
            val answer = CompletableDeferred<Unit>()
            source.onProblemset = {
                answer.await()
                throw rateLimited
            }

            problems().problemset().test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(listOf(watermelon), Instant.EPOCH, refreshing = true, error = null), awaitItem())

                answer.complete(Unit)

                assertEquals(Cached(listOf(watermelon), Instant.EPOCH, refreshing = false, error = rateLimited), awaitItem())
            }
            assertEquals(listOf(watermelon.toEntity()), database.problemDao().problems().first())
        }

    @Test
    fun `concurrent collectors trigger a single source call`() =
        runTest {
            val repository = problems()
            val response = CompletableDeferred<List<Problem>>()
            source.onProblemset = { response.await() }

            turbineScope {
                val first = repository.problemset().testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                val second = repository.problemset().testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                assertTrue(first.awaitItem().refreshing)
                assertTrue(second.awaitItem().refreshing)

                response.complete(listOf(watermelon))

                listOf(first, second).forEach { collector ->
                    collector.awaitMatching { it.data == listOf(watermelon) && !it.refreshing }
                    collector.cancel()
                }
            }
            assertEquals(1, source.problemsetCalls)
        }

    @Test
    fun `refresh skips a fresh cache unless forced`() =
        runTest {
            val repository = problems()
            database.problemDao().replaceAll(listOf(watermelon.toEntity()), currentTime)
            source.onProblemset = { listOf(theatreSquare) }

            repository.refresh()
            assertEquals(0, source.problemsetCalls)

            repository.refresh(force = true)
            assertEquals(1, source.problemsetCalls)
            assertEquals(listOf(theatreSquare.toEntity()), database.problemDao().problems().first())
        }

    @Test
    fun `a later successful refresh clears the error`() =
        runTest {
            val repository = contests()
            source.onContests = { throw JudgeError.Unavailable() }
            repository.refresh(gym = true)
            source.onContests = { listOf(gym) }

            repository.refresh(gym = true, force = true)

            assertEquals(Cached(listOf(gym), Instant.EPOCH, false, null), repository.contests(gym = true).first())
            assertEquals(2, source.contestCalls)
        }

    @Test
    fun `a fetch time in the future counts as stale`() =
        runTest {
            database.problemDao().replaceAll(listOf(watermelon.toEntity()), currentTime + 1.minutes.inWholeMilliseconds)
            source.onProblemset = { listOf(theatreSquare) }

            problems().refresh()

            assertEquals(1, source.problemsetCalls)
        }

    private fun TestScope.problems() =
        CachedProblemRepository(
            source,
            database.problemDao(),
            database.cacheMetaDao(),
            SchedulerClock(testScheduler),
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )

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
        val oldRound =
            Contest(
                ContestId(1),
                "Codeforces Beta Round 1",
                ContestPhase.Finished,
                Instant.ofEpochSecond(1_266_588_000L),
                Duration.ofHours(2),
                false,
            )
        val newRound =
            Contest(
                ContestId(2100),
                "Codeforces Round 1000",
                ContestPhase.Upcoming,
                Instant.ofEpochSecond(1_800_000_000L),
                Duration.ofHours(2),
                false,
            )
        val gym = Contest(ContestId(100_001), "Training gym", ContestPhase.Finished, null, Duration.ofHours(5), true)
        val watermelon = Problem(ProblemId(4, "A"), "Watermelon", 800, listOf("math"), 400_000)
        val theatreSquare = Problem(ProblemId(1, "A"), "Theatre Square", 1000, listOf("math"), 300_000)
    }
}
