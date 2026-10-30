package app.pocketcp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import app.pocketcp.core.model.OptionalModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val file: File get() = tmp.root.resolve("settings.preferences_pb")

    /** The production store setup (with its corruption handler) on a temp file, living as long as the test. */
    private fun TestScope.dataStore(): DataStore<Preferences> = settingsDataStore(backgroundScope) { file }

    @Test
    fun `an empty store yields the defaults`() =
        runTest {
            val settings = DataStoreSettingsRepository(dataStore()).settings.first()

            assertEquals(UserSettings(), settings)
            assertEquals(ThemePreference.System, settings.theme)
            assertEquals(TextSizePreference.Default, settings.appTextSize)
            assertEquals(TextSizePreference.Default, settings.codeTextSize)
            assertEquals(true, settings.codeLigatures)
            assertEquals(DensityPreference.Comfortable, settings.density)
            assertEquals(emptySet<OptionalModule>(), settings.disabledModules)
        }

    @Test
    fun `each setter round-trips`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            repository.settings.test {
                var expected = UserSettings()
                assertEquals(expected, awaitItem())

                repository.setTheme(ThemePreference.Amoled)
                expected = expected.copy(theme = ThemePreference.Amoled)
                assertEquals(expected, awaitItem())

                repository.setAppTextSize(TextSizePreference.Largest)
                expected = expected.copy(appTextSize = TextSizePreference.Largest)
                assertEquals(expected, awaitItem())

                repository.setCodeTextSize(TextSizePreference.Small)
                expected = expected.copy(codeTextSize = TextSizePreference.Small)
                assertEquals(expected, awaitItem())

                repository.setCodeLigatures(false)
                expected = expected.copy(codeLigatures = false)
                assertEquals(expected, awaitItem())

                repository.setDensity(DensityPreference.Compact)
                expected = expected.copy(density = DensityPreference.Compact)
                assertEquals(expected, awaitItem())

                repository.setModuleEnabled(OptionalModule.Ai, enabled = false)
                expected = expected.copy(disabledModules = setOf(OptionalModule.Ai))
                assertEquals(expected, awaitItem())
            }
        }

    @Test
    fun `enums are stored by their stable keys`() =
        runTest {
            val store = dataStore()
            val repository = DataStoreSettingsRepository(store)

            repository.setTheme(ThemePreference.Amoled)
            repository.setAppTextSize(TextSizePreference.Large)
            repository.setDensity(DensityPreference.Compact)
            repository.setModuleEnabled(OptionalModule.PracticeAndStreaks, enabled = false)

            val raw = store.data.first()
            assertEquals("amoled", raw[stringPreferencesKey("theme")])
            assertEquals("large", raw[stringPreferencesKey("app_text_size")])
            assertEquals("compact", raw[stringPreferencesKey("density")])
            assertEquals(setOf("practice_streaks"), raw[stringSetPreferencesKey("disabled_modules")])
        }

    @Test
    fun `an unknown stored key falls back to the default`() =
        runTest {
            val store = dataStore()
            store.edit {
                it[stringPreferencesKey("theme")] = "neon"
                // An enum name is not a key: stored names never match.
                it[stringPreferencesKey("app_text_size")] = "Large"
                it[stringPreferencesKey("code_text_size")] = ""
                it[stringPreferencesKey("density")] = "1"
                it[stringSetPreferencesKey("disabled_modules")] = setOf("removed_module", "ai")
            }

            val settings = DataStoreSettingsRepository(store).settings.first()

            assertEquals(UserSettings(disabledModules = setOf(OptionalModule.Ai)), settings)
        }

    @Test
    fun `toggling a module keeps unknown disabled keys`() =
        runTest {
            val store = dataStore()
            val disabledKey = stringSetPreferencesKey("disabled_modules")
            store.edit { it[disabledKey] = setOf("removed_module") }

            DataStoreSettingsRepository(store).setModuleEnabled(OptionalModule.Stats, enabled = false)

            assertEquals(setOf("removed_module", "stats"), store.data.first()[disabledKey])
        }

    @Test
    fun `a corrupted file yields the defaults and is replaced`() =
        runTest {
            // A length-delimited field claiming 127 bytes with none following: not a valid preferences proto.
            file.writeBytes(byteArrayOf(0x0A, 0x7F))
            val repository = DataStoreSettingsRepository(dataStore())

            assertEquals(UserSettings(), repository.settings.first())

            repository.setTheme(ThemePreference.Light)
            assertEquals(ThemePreference.Light, repository.settings.first().theme)
        }

    @Test
    fun `disabling and re-enabling a module`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            repository.settings.test {
                assertEquals(emptySet<OptionalModule>(), awaitItem().disabledModules)

                repository.setModuleEnabled(OptionalModule.Ai, enabled = false)
                assertEquals(setOf(OptionalModule.Ai), awaitItem().disabledModules)

                repository.setModuleEnabled(OptionalModule.Stats, enabled = false)
                assertEquals(setOf(OptionalModule.Ai, OptionalModule.Stats), awaitItem().disabledModules)

                repository.setModuleEnabled(OptionalModule.Ai, enabled = true)
                assertEquals(setOf(OptionalModule.Stats), awaitItem().disabledModules)

                // Enabling an enabled module changes nothing, so nothing is emitted.
                repository.setModuleEnabled(OptionalModule.Ai, enabled = true)
                expectNoEvents()
            }
        }
}
