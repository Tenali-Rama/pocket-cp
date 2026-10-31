package app.pocketcp.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Each migration from the previous version's committed schema in `schemas/`. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PocketCpDatabase::class.java)

    @Test
    fun `1 to 2 adds kind as Unknown and drops only the contest cache times`() =
        runTest {
            helper.createDatabase(NAME, 1).use { db ->
                db.insertContest(withKind = false)
                db.insertMeta(CacheKeys.CONTESTS, CacheKeys.GYM_CONTESTS, CacheKeys.PROBLEMSET)
            }

            helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2).close()

            withMigrated { migrated ->
                assertEquals(listOf(round960KindUnknown), migrated.contestDao().contests(gym = false).first())
                assertNull(migrated.cacheMetaDao().fetchedAt(CacheKeys.CONTESTS))
                assertNull(migrated.cacheMetaDao().fetchedAt(CacheKeys.GYM_CONTESTS))
                assertEquals(1L, migrated.cacheMetaDao().fetchedAt(CacheKeys.PROBLEMSET))
            }
        }

    @Test
    fun `2 to 3 keeps every cached row and adds a working statements table`() =
        runTest {
            helper.createDatabase(NAME, 2).use { db ->
                db.insertContest(withKind = true)
                db.insertProblem()
                db.insertMeta(CacheKeys.CONTESTS, CacheKeys.GYM_CONTESTS, CacheKeys.PROBLEMSET)
            }

            helper.runMigrationsAndValidate(NAME, 3, true, MIGRATION_2_3).close()

            withMigrated { migrated ->
                assertEquals(listOf(round960), migrated.contestDao().contests(gym = false).first())
                assertEquals(listOf(watermelon), migrated.problemDao().problems().first())
                listOf(CacheKeys.CONTESTS, CacheKeys.GYM_CONTESTS, CacheKeys.PROBLEMSET).forEach { key ->
                    assertEquals(1L, migrated.cacheMetaDao().fetchedAt(key))
                }
                assertStatementsWork(migrated)
            }
        }

    @Test
    fun `1 to 2 to 3 succeeds`() =
        runTest {
            helper.createDatabase(NAME, 1).use { db ->
                db.insertContest(withKind = false)
                db.insertProblem()
                db.insertMeta(CacheKeys.PROBLEMSET)
            }

            helper.runMigrationsAndValidate(NAME, 3, true, MIGRATION_1_2, MIGRATION_2_3).close()

            withMigrated { migrated ->
                assertEquals(listOf(round960KindUnknown), migrated.contestDao().contests(gym = false).first())
                assertEquals(listOf(watermelon), migrated.problemDao().problems().first())
                assertEquals(1L, migrated.cacheMetaDao().fetchedAt(CacheKeys.PROBLEMSET))
                assertStatementsWork(migrated)
            }
        }

    private suspend fun assertStatementsWork(database: PocketCpDatabase) {
        val statement = StatementEntity(4, "A", "en", "{}", formatVersion = 1, fetchedAtEpochMillis = 2L)
        database.statementDao().upsert(statement)
        assertEquals(statement, database.statementDao().statement(4, "A", "en").first())
        assertEquals(listOf("A"), database.statementDao().cachedIndices(4, "en", formatVersion = 1))
    }

    /** Opens the migrated file as the app does, with every migration registered. */
    private suspend fun withMigrated(block: suspend (PocketCpDatabase) -> Unit) {
        val migrated =
            Room
                .databaseBuilder(RuntimeEnvironment.getApplication(), PocketCpDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        try {
            block(migrated)
        } finally {
            migrated.close()
        }
    }

    private fun SupportSQLiteDatabase.insertContest(withKind: Boolean) {
        val (columns, values) = if (withKind) ", kind" to ", 'Div2'" else "" to ""
        execSQL(
            "INSERT INTO contests (id, name, phase, startEpochSeconds, durationSeconds, isGym$columns) " +
                "VALUES (2000, 'Codeforces Round 960', 'Finished', 1721000000, 7200, 0$values)",
        )
    }

    private fun SupportSQLiteDatabase.insertProblem() =
        execSQL(
            "INSERT INTO problems (contestId, `index`, name, rating, tags, solvedCount) " +
                "VALUES (4, 'A', 'Watermelon', 800, '[\"math\"]', 450000)",
        )

    private fun SupportSQLiteDatabase.insertMeta(vararg keys: String) =
        keys.forEach { key ->
            execSQL("INSERT INTO cache_meta (`key`, fetchedAtEpochMillis) VALUES (?, 1)", arrayOf(key))
        }

    private companion object {
        const val NAME = "migration-test.db"
        val round960 = ContestEntity(2000, "Codeforces Round 960", "Finished", 1_721_000_000L, 7_200L, false, "Div2")
        val round960KindUnknown = round960.copy(kind = "Unknown")
        val watermelon = ProblemEntity(4, "A", "Watermelon", 800, listOf("math"), 450_000)
    }
}
