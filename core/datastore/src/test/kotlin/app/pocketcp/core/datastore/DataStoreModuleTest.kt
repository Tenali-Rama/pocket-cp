package app.pocketcp.core.datastore

import app.pocketcp.core.model.OptionalModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** One test only: the singleton store stays open for the JVM, and DataStore allows one instance per file. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DataStoreModuleTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var repository: SettingsRepository

    @Inject lateinit var sameRepository: SettingsRepository

    @Inject lateinit var installedModules: Set<@JvmSuppressWildcards OptionalModule>

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `provides one settings repository with defaults and no installed modules in M1`() =
        runTest {
            assertSame(repository, sameRepository)
            assertEquals(UserSettings(), repository.settings.first())
            assertEquals(emptySet<OptionalModule>(), installedModules)
        }
}
