package app.pocketcp.core.database

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DatabaseModuleTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var database: PocketCpDatabase

    @Inject lateinit var sameDatabase: PocketCpDatabase

    @Inject lateinit var contestDao: ContestDao

    @Inject lateinit var problemDao: ProblemDao

    @Inject lateinit var cacheMetaDao: CacheMetaDao

    @Before
    fun inject() = hilt.inject()

    @After
    fun close() = database.close()

    @Test
    fun `database is an app-wide singleton and the DAOs come from it`() {
        assertSame(database, sameDatabase)
        assertSame(database.contestDao(), contestDao)
        assertSame(database.problemDao(), problemDao)
        assertSame(database.cacheMetaDao(), cacheMetaDao)
    }
}
