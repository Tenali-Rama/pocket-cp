package app.pocketcp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.pocketcp.core.model.OptionalModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** [SettingsRepository] on a Preferences [DataStore]. Enums are stored by their [StoredPreference.key]. */
internal class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<UserSettings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toUserSettings() }
            .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemePreference) = put(Keys.THEME, theme.key)

    override suspend fun setAppTextSize(size: TextSizePreference) = put(Keys.APP_TEXT_SIZE, size.key)

    override suspend fun setCodeTextSize(size: TextSizePreference) = put(Keys.CODE_TEXT_SIZE, size.key)

    override suspend fun setCodeLigatures(enabled: Boolean) = put(Keys.CODE_LIGATURES, enabled)

    override suspend fun setDensity(density: DensityPreference) = put(Keys.DENSITY, density.key)

    override suspend fun setModuleEnabled(
        module: OptionalModule,
        enabled: Boolean,
    ) {
        dataStore.edit { prefs ->
            // Edits the stored keys, so a key this version doesn't know (e.g. written by a newer build) survives.
            val disabled = prefs[Keys.DISABLED_MODULES].orEmpty()
            prefs[Keys.DISABLED_MODULES] = if (enabled) disabled - module.key else disabled + module.key
        }
    }

    private suspend fun <T> put(
        key: Preferences.Key<T>,
        value: T,
    ) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            theme = stored(Keys.THEME, defaults.theme),
            appTextSize = stored(Keys.APP_TEXT_SIZE, defaults.appTextSize),
            codeTextSize = stored(Keys.CODE_TEXT_SIZE, defaults.codeTextSize),
            codeLigatures = this[Keys.CODE_LIGATURES] ?: defaults.codeLigatures,
            density = stored(Keys.DENSITY, defaults.density),
            disabledModules = this[Keys.DISABLED_MODULES].orEmpty().mapNotNull(OptionalModule::fromKey).toSet(),
        )
    }

    /** The entry of [default]'s enum stored under [key], or [default] when unset or unknown. */
    private inline fun <reified E> Preferences.stored(
        key: Preferences.Key<String>,
        default: E,
    ): E where E : Enum<E>, E : StoredPreference {
        val stored = this[key] ?: return default
        return enumValues<E>().firstOrNull { it.key == stored } ?: default
    }

    /** Preference names are persisted: never rename one. */
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val APP_TEXT_SIZE = stringPreferencesKey("app_text_size")
        val CODE_TEXT_SIZE = stringPreferencesKey("code_text_size")
        val CODE_LIGATURES = booleanPreferencesKey("code_ligatures")
        val DENSITY = stringPreferencesKey("density")
        val DISABLED_MODULES = stringSetPreferencesKey("disabled_modules")
    }
}
