package app.pocketcp.core.datastore

import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.flow.Flow

/** The user's settings. Read with [settings]; each setter persists one field. */
interface SettingsRepository {
    /**
     * The current settings, then every change. Never fails on IO: an unreadable store yields the defaults,
     * and later changes are still emitted once the store reads again.
     */
    val settings: Flow<UserSettings>

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setAppTextSize(size: TextSizePreference)

    suspend fun setCodeTextSize(size: TextSizePreference)

    suspend fun setCodeLigatures(enabled: Boolean)

    suspend fun setDensity(density: DensityPreference)

    suspend fun setHideProblemTags(hidden: Boolean)

    suspend fun setHideProblemRatings(hidden: Boolean)

    suspend fun setStatementLanguage(language: StatementLanguage)

    suspend fun setStatementTextSize(size: TextSizePreference)

    suspend fun setStatementWidth(width: StatementWidthPreference)

    /** Turns [module] on or off in `disabledModules`; the other modules keep their state. */
    suspend fun setModuleEnabled(
        module: OptionalModule,
        enabled: Boolean,
    )
}
