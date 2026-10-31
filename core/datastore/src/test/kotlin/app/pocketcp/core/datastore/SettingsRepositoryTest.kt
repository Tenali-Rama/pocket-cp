package app.pocketcp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

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
            assertEquals(false, settings.hideProblemTags)
            assertEquals(false, settings.hideProblemRatings)
            assertEquals(StatementLanguage.En, settings.statementLanguage)
            assertEquals(TextSizePreference.Default, settings.statementTextSize)
            assertEquals(StatementWidthPreference.Comfortable, settings.statementWidth)
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

                repository.setHideProblemTags(true)
                expected = expected.copy(hideProblemTags = true)
                assertEquals(expected, awaitItem())

                repository.setHideProblemRatings(true)
                expected = expected.copy(hideProblemRatings = true)
                assertEquals(expected, awaitItem())

                repository.setHideProblemTags(false)
                expected = expected.copy(hideProblemTags = false)
                assertEquals(expected, awaitItem())

                repository.setStatementLanguage(StatementLanguage.Ru)
                expected = expected.copy(statementLanguage = StatementLanguage.Ru)
                assertEquals(expected, awaitItem())

                repository.setStatementTextSize(TextSizePreference.Large)
                expected = expected.copy(statementTextSize = TextSizePreference.Large)
                assertEquals(expected, awaitItem())

                repository.setStatementWidth(StatementWidthPreference.Full)
                expected = expected.copy(statementWidth = StatementWidthPreference.Full)
                assertEquals(expected, awaitItem())

                repository.setStatementWidth(StatementWidthPreference.Narrow)
                expected = expected.copy(statementWidth = StatementWidthPreference.Narrow)
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
            repository.setHideProblemTags(true)
            repository.setHideProblemRatings(true)
            repository.setStatementLanguage(StatementLanguage.Ru)
            repository.setStatementTextSize(TextSizePreference.Small)
            repository.setStatementWidth(StatementWidthPreference.Narrow)

            val raw = store.data.first()
            assertEquals("amoled", raw[stringPreferencesKey("theme")])
            assertEquals("large", raw[stringPreferencesKey("app_text_size")])
            assertEquals("compact", raw[stringPreferencesKey("density")])
            assertEquals(setOf("practice_streaks"), raw[stringSetPreferencesKey("disabled_modules")])
            assertEquals(true, raw[booleanPreferencesKey("hide_problem_tags")])
            assertEquals(true, raw[booleanPreferencesKey("hide_problem_ratings")])
            assertEquals("ru", raw[stringPreferencesKey("statement_language")])
            assertEquals("small", raw[stringPreferencesKey("statement_text_size")])
            assertEquals("narrow", raw[stringPreferencesKey("statement_width")])
        }

    @Test
    fun `reading settings written by an earlier launch are read back`() =
        runTest {
            val store = dataStore()
            store.edit {
                it[stringPreferencesKey("statement_language")] = "ru"
                it[stringPreferencesKey("statement_text_size")] = "largest"
                it[stringPreferencesKey("statement_width")] = "full"
            }

            val settings = DataStoreSettingsRepository(store).settings.first()

            assertEquals(
                UserSettings(
                    statementLanguage = StatementLanguage.Ru,
                    statementTextSize = TextSizePreference.Largest,
                    statementWidth = StatementWidthPreference.Full,
                ),
                settings,
            )
        }

    @Test
    fun `a spoiler setting written by an earlier launch is read back`() =
        runTest {
            val store = dataStore()
            store.edit {
                it[booleanPreferencesKey("hide_problem_tags")] = true
                it[booleanPreferencesKey("hide_problem_ratings")] = true
            }

            val settings = DataStoreSettingsRepository(store).settings.first()

            assertEquals(UserSettings(hideProblemTags = true, hideProblemRatings = true), settings)
        }

    @Test
    fun `after a read IOException the defaults are emitted and later writes still arrive`() =
        runTest {
            val repository = DataStoreSettingsRepository(FirstReadFails(dataStore(), IOException("disk unreadable")))
            repository.settings.test {
                assertEquals(UserSettings(), awaitItem())

                repository.setHideProblemRatings(true)
                assertEquals(UserSettings(hideProblemRatings = true), awaitItem())

                repository.setTheme(ThemePreference.Dark)
                assertEquals(UserSettings(hideProblemRatings = true, theme = ThemePreference.Dark), awaitItem())
            }
        }

    @Test
    fun `a non-IO read error propagates`() =
        runTest {
            val error = IllegalStateException("bug")
            val repository = DataStoreSettingsRepository(FirstReadFails(dataStore(), error))
            repository.settings.test {
                val thrown = awaitError()
                assertEquals(IllegalStateException::class, thrown::class)
                assertEquals(error.message, thrown.message)
            }
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
                it[stringPreferencesKey("statement_language")] = "de"
                it[stringPreferencesKey("statement_text_size")] = "Small"
                it[stringPreferencesKey("statement_width")] = "wide"
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

    /** Delegates to [store], except that the first collection of [data] fails with [error]. */
    private class FirstReadFails(
        private val store: DataStore<Preferences>,
        private val error: Throwable,
    ) : DataStore<Preferences> {
        private var reads = 0

        override val data: Flow<Preferences> =
            flow {
                if (reads++ == 0) throw error
                emitAll(store.data)
            }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = store.updateData(transform)
    }
}
