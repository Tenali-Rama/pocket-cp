package app.pocketcp.feature.settings

import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.OptionalModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** An in-memory [SettingsRepository]: [stored] is what the store holds. */
class FakeSettingsRepository(
    initial: UserSettings = UserSettings(),
) : SettingsRepository {
    val stored = MutableStateFlow(initial)

    override val settings = stored

    override suspend fun setTheme(theme: ThemePreference) = stored.update { it.copy(theme = theme) }

    override suspend fun setAppTextSize(size: TextSizePreference) = stored.update { it.copy(appTextSize = size) }

    override suspend fun setCodeTextSize(size: TextSizePreference) = stored.update { it.copy(codeTextSize = size) }

    override suspend fun setCodeLigatures(enabled: Boolean) = stored.update { it.copy(codeLigatures = enabled) }

    override suspend fun setDensity(density: DensityPreference) = stored.update { it.copy(density = density) }

    override suspend fun setHideProblemTags(hidden: Boolean) = stored.update { it.copy(hideProblemTags = hidden) }

    override suspend fun setHideProblemRatings(hidden: Boolean) = stored.update { it.copy(hideProblemRatings = hidden) }

    override suspend fun setModuleEnabled(
        module: OptionalModule,
        enabled: Boolean,
    ) = stored.update {
        it.copy(disabledModules = if (enabled) it.disabledModules - module else it.disabledModules + module)
    }
}
