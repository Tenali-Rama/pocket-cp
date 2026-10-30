package app.pocketcp.feature.settings

import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) { ProvideWindowLayout(content) }
}

/** Three installed modules, Stats off. */
private val loaded =
    SettingsUiState(
        settings =
            UserSettings(
                theme = ThemePreference.System,
                appTextSize = TextSizePreference.Large,
                density = DensityPreference.Comfortable,
                disabledModules = setOf(OptionalModule.Stats),
            ),
        installedModules = listOf(OptionalModule.Ai, OptionalModule.StressTester, OptionalModule.Stats),
    )

private val NoActions =
    object : SettingsActions {
        override fun setTheme(theme: ThemePreference) = Unit

        override fun setAppTextSize(size: TextSizePreference) = Unit

        override fun setCodeTextSize(size: TextSizePreference) = Unit

        override fun setCodeLigatures(enabled: Boolean) = Unit

        override fun setDensity(density: DensityPreference) = Unit

        override fun setModuleEnabled(
            module: OptionalModule,
            enabled: Boolean,
        ) = Unit
    }

/**
 * `settings_<device>_<theme>`: nothing opened (phone: the list; wider: list | Appearance).
 * `settings_appearance_phone_*`, `settings_features_tablet_*`, `settings_appearance_font150_phone_dark`,
 * `settings_loading_phone_dark`.
 */
class SettingsScreenshotTest : ScreenshotTest() {
    @Test
    fun settings() =
        captureMatrix("settings", themeWrapper = PocketCpThemeWrapper) {
            SettingsScreen(loaded, NoActions, selected = null, onSelect = {})
        }

    @Test
    fun appearancePhoneDark() = appearancePhone(dark = true)

    @Test
    fun appearancePhoneLight() = appearancePhone(dark = false)

    @Test
    fun featuresTabletDark() = featuresTablet(dark = true)

    @Test
    fun featuresTabletLight() = featuresTablet(dark = false)

    @Test
    fun appearanceLargeFont() =
        captureSingle("settings_appearance_font150", ScreenshotConfig(fontScale = 1.5f), PocketCpThemeWrapper) {
            SettingsScreen(loaded, NoActions, selected = SettingsCategory.Appearance, onSelect = {})
        }

    @Test
    fun loading() =
        captureSingle("settings_loading", ScreenshotConfig(), PocketCpThemeWrapper) {
            SettingsScreen(null, NoActions, selected = null, onSelect = {})
        }

    private fun appearancePhone(dark: Boolean) =
        captureSingle("settings_appearance", ScreenshotConfig(dark = dark), PocketCpThemeWrapper) {
            SettingsScreen(loaded, NoActions, selected = SettingsCategory.Appearance, onSelect = {})
        }

    private fun featuresTablet(dark: Boolean) =
        captureSingle("settings_features", ScreenshotConfig(Device.Tablet, dark), PocketCpThemeWrapper) {
            SettingsScreen(loaded, NoActions, selected = SettingsCategory.Features, onSelect = {})
        }
}
