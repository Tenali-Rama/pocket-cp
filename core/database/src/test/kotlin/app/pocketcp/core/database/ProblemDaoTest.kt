package app.pocketcp.core.database

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProblemDaoTest {
    private lateinit var database: PocketCpDatabase
    private lateinit var dao: ProblemDao
    private lateinit var meta: CacheMetaDao

    @Before
    fun open() {
        database = inMemoryDatabase()
        dao = database.problemDao()
        meta = database.cacheMetaDao()
    }

    @After
    fun close() = database.close()

    @Test
    fun `flow re-emits after replaceAll, newest contest first then by index`() =
        runTest {
            dao.problems().test {
                assertEquals(emptyList<ProblemEntity>(), awaitItem())

                dao.replaceAll(listOf(watermelon, newB, newA), fetchedAtEpochMillis = 1L)

                assertEquals(listOf(newA, newB, watermelon), awaitItem())
            }
        }

    @Test
    fun `replaceAll removes problems missing from the new set and records fetchedAt`() =
        runTest {
            dao.replaceAll(listOf(watermelon, newA), fetchedAtEpochMillis = 1L)

            dao.replaceAll(listOf(newB), fetchedAtEpochMillis = 2L)

            assertEquals(listOf(newB), dao.problems().first())
            assertEquals(2L, meta.fetchedAt(CacheKeys.PROBLEMSET))
        }

    @Test
    fun `tags round-trip, including an empty list`() =
        runTest {
            dao.replaceAll(listOf(watermelon, newA), fetchedAtEpochMillis = 1L)

            val tags = dao.problems().first().associate { it.contestId to it.tags }
            assertEquals(listOf("brute force", "math", "*special"), tags[4])
            assertEquals(emptyList<String>(), tags[2000])
        }

    @Test
    fun `a null rating and solved count stay null`() =
        runTest {
            dao.replaceAll(listOf(newA), fetchedAtEpochMillis = 1L)

            val stored = dao.problems().first().single()
            assertEquals(newA, stored)
            assertNull(stored.rating)
            assertNull(stored.solvedCount)
        }

    @Test
    fun `fetchedAt is null before the first replaceAll`() =
        runTest {
            assertNull(meta.fetchedAt(CacheKeys.PROBLEMSET))
        }

    private companion object {
        val watermelon = ProblemEntity(4, "A", "Watermelon", 800, listOf("brute force", "math", "*special"), 450_000)
        val newA = ProblemEntity(2000, "A", "Fresh problem", rating = null, tags = emptyList(), solvedCount = null)
        val newB = ProblemEntity(2000, "B1", "Easy version", 1_200, listOf("greedy, constructive"), 9_000)
    }
}
