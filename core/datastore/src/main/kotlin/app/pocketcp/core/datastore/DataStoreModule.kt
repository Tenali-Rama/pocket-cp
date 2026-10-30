package app.pocketcp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.pocketcp.core.model.OptionalModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Singleton

/**
 * The settings repository, and the `Set<OptionalModule>` of installed modules: each optional module
 * contributes itself with `@IntoSet`, and Settings lists the set (empty in M1).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Multibinds
    abstract fun installedModules(): Set<OptionalModule>

    companion object {
        /** The store's name; its file is `files/datastore/settings.preferences_pb`. */
        const val SETTINGS_NAME = "settings"

        /** Singleton: DataStore allows only one open instance per file. */
        @Provides
        @Singleton
        fun provideSettingsRepository(
            @ApplicationContext context: Context,
        ): SettingsRepository =
            DataStoreSettingsRepository(
                settingsDataStore(dataStoreScope()) { context.preferencesDataStoreFile(SETTINGS_NAME) },
            )

        /** The store's app-lifetime scope. The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks. */
        private fun dataStoreScope(dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatcher)
    }
}

/** A Preferences store that replaces a corrupted file with empty preferences, so settings fall back to defaults. */
internal fun settingsDataStore(
    scope: CoroutineScope,
    produceFile: () -> File,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )
