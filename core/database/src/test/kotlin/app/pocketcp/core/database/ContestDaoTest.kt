package app.pocketcp.core.database

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContestDaoTest {
    private lateinit var database: PocketCpDatabase
    private lateinit var dao: ContestDao
    private lateinit var meta: CacheMetaDao

    @Before
    fun open() {
        database = inMemoryDatabase()
        dao = database.contestDao()
        meta = database.cacheMetaDao()
    }

    @After
    fun close() = database.close()

    @Test
    fun `flow re-emits after replaceAll, newest start first`() =
        runTest {
            dao.contests(gym = false).test(timeout = FLOW_TIMEOUT) {
                assertEquals(emptyList<ContestEntity>(), awaitItem())

                dao.replaceAll(gym = false, contests = listOf(old, upcoming, recent), fetchedAtEpochMillis = 1L)

                assertEquals(listOf(upcoming, recent, old), awaitItem())
            }
        }

    @Test
    fun `replaceAll removes contests missing from the new set`() =
        runTest {
            dao.replaceAll(gym = false, contests = listOf(old, recent), fetchedAtEpochMillis = 1L)

            dao.replaceAll(gym = false, contests = listOf(recent, upcoming), fetchedAtEpochMillis = 2L)

            assertEquals(listOf(upcoming, recent), dao.contests(gym = false).first())
        }

    @Test
    fun `replaceAll swaps one dataset and leaves the other`() =
        runTest {
            dao.replaceAll(gym = false, contests = listOf(recent), fetchedAtEpochMillis = 1L)
            dao.replaceAll(gym = true, contests = listOf(gymNoStart), fetchedAtEpochMillis = 2L)

            dao.replaceAll(gym = false, contests = emptyList(), fetchedAtEpochMillis = 3L)

            assertEquals(emptyList<ContestEntity>(), dao.contests(gym = false).first())
            assertEquals(listOf(gymNoStart), dao.contests(gym = true).first())
            assertEquals(3L, meta.fetchedAt(CacheKeys.CONTESTS))
            assertEquals(2L, meta.fetchedAt(CacheKeys.GYM_CONTESTS))
        }

    @Test
    fun `contests without a start time sort last and keep a null start`() =
        runTest {
            val gymStarted = gymNoStart.copy(id = 100_002, startEpochSeconds = 1_600_000_000L)

            dao.replaceAll(gym = true, contests = listOf(gymNoStart, gymStarted), fetchedAtEpochMillis = 1L)

            val stored = dao.contests(gym = true).first()
            assertEquals(listOf(gymStarted, gymNoStart), stored)
            assertNull(stored.last().startEpochSeconds)
        }

    @Test
    fun `fetchedAt is null before the first replaceAll`() =
        runTest {
            assertNull(meta.fetchedAt(CacheKeys.CONTESTS))
        }

    @Test
    fun `replaceAll rejects a contest from the other dataset and writes nothing`() =
        runTest {
            val error = runCatching { dao.replaceAll(gym = false, contests = listOf(gymNoStart), fetchedAtEpochMillis = 1L) }

            assertTrue(error.exceptionOrNull() is IllegalArgumentException)
            assertEquals(emptyList<ContestEntity>(), dao.contests(gym = true).first())
            assertNull(meta.fetchedAt(CacheKeys.CONTESTS))
        }

    @Test
    fun `activeContests gives every running and the earliest upcoming regular contests, in order`() =
        runTest {
            val finished = (1..2000).map { regular(it, "Finished", start = 1_000_000L + it) }
            val running = listOf(regular(3002, "Running", start = 50L), regular(3001, "Running", start = 50L))
            val upcoming = (1..8).map { regular(4000 + it, "Upcoming", start = 10_000L - it) }
            val gyms =
                listOf(
                    regular(100_001, "Running", start = 1L).copy(isGym = true),
                    regular(100_002, "Upcoming", start = 1L).copy(isGym = true),
                )
            dao.replaceAll(gym = false, (finished + upcoming + running).reversed(), fetchedAtEpochMillis = 1L)
            dao.replaceAll(gym = true, contests = gyms, fetchedAtEpochMillis = 1L)

            val active = dao.activeContests(5).first()

            val earliestUpcoming = upcoming.sortedBy { it.startEpochSeconds }.take(5)
            assertEquals(running.sortedBy { it.id } + earliestUpcoming, active)
        }

    @Test
    fun `an upcoming contest without a start comes after the dated ones`() =
        runTest {
            val noStart = regular(10, "Upcoming", start = null)
            val later = regular(11, "Upcoming", start = 2_000L)
            val sooner = regular(12, "Upcoming", start = 1_000L)
            dao.replaceAll(gym = false, contests = listOf(noStart, later, sooner), fetchedAtEpochMillis = 1L)

            assertEquals(listOf(sooner, later, noStart), dao.activeContests(5).first())
            assertEquals(listOf(sooner, later), dao.activeContests(2).first())
        }

    @Test
    fun `activeContests(0) gives only the running contests`() =
        runTest {
            val running = regular(11, "Running", start = 100L)
            val unknown = regular(12, "Unknown", start = 100L)
            dao.replaceAll(
                gym = false,
                contests = listOf(regular(13, "Upcoming", start = 200L), running, unknown, old),
                fetchedAtEpochMillis = 1L,
            )

            assertEquals(listOf(running), dao.activeContests(0).first())
        }

    @Test
    fun `activeContests re-emits after replaceAll of the regular dataset`() =
        runTest {
            dao.activeContests(5).test(timeout = FLOW_TIMEOUT) {
                assertEquals(emptyList<ContestEntity>(), awaitItem())

                dao.replaceAll(gym = false, contests = listOf(old, upcoming), fetchedAtEpochMillis = 1L)

                assertEquals(listOf(upcoming), awaitItem())
            }
        }

    private fun regular(
        id: Int,
        phase: String,
        start: Long?,
    ) = ContestEntity(id, "Round $id", phase, start, 7_200L, isGym = false, kind = "Cf")

    private companion object {
        val old =
            ContestEntity(1, "Codeforces Beta Round 1", "Finished", 1_266_588_000L, 7_200L, isGym = false, kind = "Cf")
        val recent =
            ContestEntity(2000, "Codeforces Round 960", "Finished", 1_721_000_000L, 7_200L, isGym = false, kind = "Cf")
        val upcoming =
            ContestEntity(2100, "Codeforces Round 1000", "Upcoming", 1_800_000_000L, 9_000L, isGym = false, kind = "Cf")
        val gymNoStart = ContestEntity(100_001, "Training gym", "Finished", null, 18_000L, isGym = true, kind = "Icpc")
    }
}
