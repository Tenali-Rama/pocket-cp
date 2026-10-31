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
class StatementDaoTest {
    private lateinit var database: PocketCpDatabase
    private lateinit var dao: StatementDao

    @Before
    fun open() {
        database = inMemoryDatabase()
        dao = database.statementDao()
    }

    @After
    fun close() = database.close()

    @Test
    fun `statement emits null, then the row after upsert, then its replacement`() =
        runTest {
            dao.statement(2000, "A", "en").test(timeout = FLOW_TIMEOUT) {
                assertNull(awaitItem())

                dao.upsert(englishA)
                assertEquals(englishA, awaitItem())

                val refreshed = englishA.copy(document = "{\"v\":2}", fetchedAtEpochMillis = 2L)
                dao.upsert(refreshed)
                assertEquals(refreshed, awaitItem())
            }
        }

    @Test
    fun `en and ru rows of one problem are separate`() =
        runTest {
            val russianA = englishA.copy(language = "ru", document = "{\"ru\":true}")

            dao.upsert(englishA)
            dao.upsert(russianA)

            assertEquals(englishA, dao.statement(2000, "A", "en").first())
            assertEquals(russianA, dao.statement(2000, "A", "ru").first())
            assertNull(dao.statement(2000, "B", "en").first())
        }

    @Test
    fun `cachedIndices ignores other contests, languages and format versions`() =
        runTest {
            listOf(
                englishA.copy(index = "B1"),
                englishA,
                englishA.copy(index = "C", language = "ru"),
                englishA.copy(index = "D", formatVersion = 2),
                englishA.copy(contestId = 2001, index = "E"),
            ).forEach { dao.upsert(it) }

            assertEquals(listOf("A", "B1"), dao.cachedIndices(2000, "en", formatVersion = 1))
            assertEquals(listOf("D"), dao.cachedIndices(2000, "en", formatVersion = 2))
            assertEquals(emptyList<String>(), dao.cachedIndices(1999, "en", formatVersion = 1))
        }

    private companion object {
        val englishA = StatementEntity(2000, "A", "en", "{\"v\":1}", formatVersion = 1, fetchedAtEpochMillis = 1L)
    }
}
