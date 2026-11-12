package app.pocketcp.shell

import android.content.res.Configuration
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.designsystem.UiDensity

/** Light only when the system says night mode is off; dark when it is on or unknown (dark-first, spec §5). */
fun themeModeOf(uiMode: Int): ThemeMode =
    when (uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_NO -> ThemeMode.Light
        else -> ThemeMode.Dark
    }

/** The user's [theme]: `System` follows [uiMode]'s night mode ([themeModeOf]); the others force their mode. */
fun themeModeOf(
    theme: ThemePreference,
    uiMode: Int,
): ThemeMode =
    when (theme) {
        ThemePreference.System -> themeModeOf(uiMode)
        ThemePreference.Dark -> ThemeMode.Dark
        ThemePreference.Light -> ThemeMode.Light
        ThemePreference.Amoled -> ThemeMode.Amoled
    }

/** The design system's density for the stored preference. */
val DensityPreference.uiDensity: UiDensity
    get() =
        when (this) {
            DensityPreference.Comfortable -> UiDensity.Comfortable
            DensityPreference.Compact -> UiDensity.Compact
        }
