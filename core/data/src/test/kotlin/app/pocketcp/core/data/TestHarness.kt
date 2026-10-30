package app.pocketcp.core.data

import androidx.room.Room
import app.cash.turbine.ReceiveTurbine
import app.pocketcp.core.database.PocketCpDatabase
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.JudgeId
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.robolectric.RuntimeEnvironment
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

/** Wall-clock wait for a Room `Flow` emission; the first query in a test JVM also loads Robolectric's SQLite. */
internal val FLOW_TIMEOUT = 15.seconds

/** A fresh in-memory database on Room's own executors, as in core/database's tests. */
internal fun inMemoryDatabase(): PocketCpDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PocketCpDatabase::class.java).build()

/** The test scheduler's virtual time as a [Clock]: `advanceTimeBy` ages the cache. Starts at the epoch. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(scheduler.currentTime)
}

/** A [JudgeSource] whose answers the test sets, counting calls. An unexpected call fails the test. */
internal class FakeJudgeSource : JudgeSource {
    override val judge = JudgeId("fake")

    var onContests: suspend (gym: Boolean) -> List<Contest> = { error("unexpected contests call") }
    var onProblemset: suspend () -> List<Problem> = { error("unexpected problemset call") }

    var contestCalls = 0
        private set
    var problemsetCalls = 0
        private set

    override suspend fun contests(gym: Boolean): List<Contest> {
        contestCalls++
        return onContests(gym)
    }

    override suspend fun problemset(): List<Problem> {
        problemsetCalls++
        return onProblemset()
    }

    override suspend fun users(handles: List<String>): List<User> = error("unexpected users call")
}

/** Skips items until one matches [predicate]; the Room write and the status change may land in either order. */
internal suspend fun <T> ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
