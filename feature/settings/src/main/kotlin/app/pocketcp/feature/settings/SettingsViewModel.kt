package app.pocketcp.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.OptionalModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long the settings stay collected after the screen stops, so a rotation does not restart the flow. */
private const val STOP_TIMEOUT_MS = 5_000L

/** A page of the Settings screen. */
enum class SettingsCategory {
    Appearance,
    Features,
}

/** What the Settings screen shows: the stored [settings] and the [installedModules] in declaration order. */
@Immutable
data class SettingsUiState(
    val settings: UserSettings,
    val installedModules: List<OptionalModule>,
) {
    /** Appearance always; Features only when an optional module is installed. */
    val categories: List<SettingsCategory> =
        if (installedModules.isEmpty()) {
            listOf(SettingsCategory.Appearance)
        } else {
            listOf(SettingsCategory.Appearance, SettingsCategory.Features)
        }
}

/** The changes the Settings screen makes. Each one is persisted through `SettingsRepository`. */
interface SettingsActions {
    fun setTheme(theme: ThemePreference)

    fun setAppTextSize(size: TextSizePreference)

    fun setCodeTextSize(size: TextSizePreference)

    fun setCodeLigatures(enabled: Boolean)

    fun setDensity(density: DensityPreference)

    fun setModuleEnabled(
        module: OptionalModule,
        enabled: Boolean,
    )
}

/**
 * The Settings screen's state, live on [repository]: `null` until the first read. Built by [SettingsEntry] for its
 * back stack entry, so it needs no Hilt view-model factory (ADR 0012).
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    installedModules: Set<OptionalModule>,
) : ViewModel(),
    SettingsActions {
    private val installed = installedModules.sorted()

    val state: StateFlow<SettingsUiState?> =
        repository.settings
            .map { SettingsUiState(it, installed) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    override fun setTheme(theme: ThemePreference) = write { setTheme(theme) }

    override fun setAppTextSize(size: TextSizePreference) = write { setAppTextSize(size) }

    override fun setCodeTextSize(size: TextSizePreference) = write { setCodeTextSize(size) }

    override fun setCodeLigatures(enabled: Boolean) = write { setCodeLigatures(enabled) }

    override fun setDensity(density: DensityPreference) = write { setDensity(density) }

    override fun setModuleEnabled(
        module: OptionalModule,
        enabled: Boolean,
    ) = write { setModuleEnabled(module, enabled) }

    private fun write(change: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repository.change() }
    }
}
