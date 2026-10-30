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
