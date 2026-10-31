package app.pocketcp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

/**
 * [SettingsRepository] on a Preferences [DataStore]. Enums are stored by their [StoredPreference.key].
 * One setter per setting, like the interface, so the function count grows with settings.
 */
@Suppress("TooManyFunctions")
internal class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    /**
     * On a read [IOException] this emits the defaults once (on the first failure), then re-reads the store
     * after a backoff that doubles from [RETRY_BASE_MS] up to [RETRY_MAX_MS], for as long as it is collected.
     * A later successful read, and every write after it, is emitted as usual. Any other error propagates.
     */
    override val settings: Flow<UserSettings> =
        dataStore.data
            .retryWhen { error, attempt ->
                if (error !is IOException) return@retryWhen false
                if (attempt == 0L) emit(emptyPreferences())
                delay(retryDelayMs(attempt))
                true
            }.map { it.toUserSettings() }
            .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemePreference) = put(Keys.THEME, theme.key)

    override suspend fun setAppTextSize(size: TextSizePreference) = put(Keys.APP_TEXT_SIZE, size.key)

    override suspend fun setCodeTextSize(size: TextSizePreference) = put(Keys.CODE_TEXT_SIZE, size.key)

    override suspend fun setCodeLigatures(enabled: Boolean) = put(Keys.CODE_LIGATURES, enabled)

    override suspend fun setDensity(density: DensityPreference) = put(Keys.DENSITY, density.key)

    override suspend fun setHideProblemTags(hidden: Boolean) = put(Keys.HIDE_PROBLEM_TAGS, hidden)

    override suspend fun setHideProblemRatings(hidden: Boolean) = put(Keys.HIDE_PROBLEM_RATINGS, hidden)

    override suspend fun setStatementLanguage(language: StatementLanguage) = put(Keys.STATEMENT_LANGUAGE, language.key)

    override suspend fun setStatementTextSize(size: TextSizePreference) = put(Keys.STATEMENT_TEXT_SIZE, size.key)

    override suspend fun setStatementWidth(width: StatementWidthPreference) = put(Keys.STATEMENT_WIDTH, width.key)

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
            hideProblemTags = this[Keys.HIDE_PROBLEM_TAGS] ?: defaults.hideProblemTags,
            hideProblemRatings = this[Keys.HIDE_PROBLEM_RATINGS] ?: defaults.hideProblemRatings,
            statementLanguage =
                this[Keys.STATEMENT_LANGUAGE]?.let(StatementLanguage::fromKey) ?: defaults.statementLanguage,
            statementTextSize = stored(Keys.STATEMENT_TEXT_SIZE, defaults.statementTextSize),
            statementWidth = stored(Keys.STATEMENT_WIDTH, defaults.statementWidth),
        )
    }

    /** Preference names are persisted: never rename one. */
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val APP_TEXT_SIZE = stringPreferencesKey("app_text_size")
        val CODE_TEXT_SIZE = stringPreferencesKey("code_text_size")
        val CODE_LIGATURES = booleanPreferencesKey("code_ligatures")
        val DENSITY = stringPreferencesKey("density")
        val DISABLED_MODULES = stringSetPreferencesKey("disabled_modules")
        val HIDE_PROBLEM_TAGS = booleanPreferencesKey("hide_problem_tags")
        val HIDE_PROBLEM_RATINGS = booleanPreferencesKey("hide_problem_ratings")
        val STATEMENT_LANGUAGE = stringPreferencesKey("statement_language")
        val STATEMENT_TEXT_SIZE = stringPreferencesKey("statement_text_size")
        val STATEMENT_WIDTH = stringPreferencesKey("statement_width")
    }
}

private const val RETRY_BASE_MS = 100L
private const val RETRY_MAX_MS = 5_000L
private const val RETRY_MAX_SHIFT = 6

/** The delay before re-reading after the [attempt]th failure: 100 ms, 200 ms, 400 ms, … capped at [RETRY_MAX_MS]. */
private fun retryDelayMs(attempt: Long): Long =
    (RETRY_BASE_MS shl attempt.coerceAtMost(RETRY_MAX_SHIFT.toLong()).toInt()).coerceAtMost(RETRY_MAX_MS)

/** The entry of [default]'s enum stored under [key], or [default] when unset or unknown. */
private inline fun <reified E> Preferences.stored(
    key: Preferences.Key<String>,
    default: E,
): E where E : Enum<E>, E : StoredPreference {
    val stored = this[key] ?: return default
    return enumValues<E>().firstOrNull { it.key == stored } ?: default
}
