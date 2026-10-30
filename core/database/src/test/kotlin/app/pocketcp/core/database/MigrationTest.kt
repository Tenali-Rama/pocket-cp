package app.pocketcp.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
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
                db.execSQL(
                    "INSERT INTO contests (id, name, phase, startEpochSeconds, durationSeconds, isGym) " +
                        "VALUES (2000, 'Codeforces Round 960', 'Finished', 1721000000, 7200, 0)",
                )
                listOf(CacheKeys.CONTESTS, CacheKeys.GYM_CONTESTS, CacheKeys.PROBLEMSET).forEach { key ->
                    db.execSQL("INSERT INTO cache_meta (`key`, fetchedAtEpochMillis) VALUES (?, 1)", arrayOf(key))
                }
            }

            helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2).close()

            val migrated =
                Room
                    .databaseBuilder(RuntimeEnvironment.getApplication(), PocketCpDatabase::class.java, NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
            try {
                val expected =
                    ContestEntity(2000, "Codeforces Round 960", "Finished", 1_721_000_000L, 7_200L, false, "Unknown")
                assertEquals(listOf(expected), migrated.contestDao().contests(gym = false).first())
                assertNull(migrated.cacheMetaDao().fetchedAt(CacheKeys.CONTESTS))
                assertNull(migrated.cacheMetaDao().fetchedAt(CacheKeys.GYM_CONTESTS))
                assertEquals(1L, migrated.cacheMetaDao().fetchedAt(CacheKeys.PROBLEMSET))
            } finally {
                migrated.close()
            }
        }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
