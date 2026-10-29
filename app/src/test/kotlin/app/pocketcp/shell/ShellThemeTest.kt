package app.pocketcp.shell

import android.content.res.Configuration
import app.pocketcp.core.designsystem.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** The shell follows the system night mode, and is dark when the system does not say. */
class ShellThemeTest {
    @Test
    fun systemDark_isDark() {
        assertEquals(ThemeMode.Dark, themeModeOf(Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES))
    }

    @Test
    fun systemLight_isLight() {
        assertEquals(ThemeMode.Light, themeModeOf(Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO))
    }

    @Test
    fun systemUnspecified_isDark() {
        assertEquals(ThemeMode.Dark, themeModeOf(Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_UNDEFINED))
        assertEquals(ThemeMode.Dark, themeModeOf(Configuration.UI_MODE_TYPE_UNDEFINED))
    }
}
