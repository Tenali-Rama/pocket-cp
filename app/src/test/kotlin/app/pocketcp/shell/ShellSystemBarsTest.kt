package app.pocketcp.shell

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `onDarkTheme`: the shell reports whether the resolved theme is dark, so the system-bar icons can follow it. */
@RunWith(AndroidJUnit4::class)
class ShellSystemBarsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val night = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES
    private val day = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO

    @Test
    fun system_underNight_reportsDark() {
        assertEquals(true, lastReport(ThemePreference.System, night))
    }

    @Test
    fun system_underDay_reportsLight() {
        assertEquals(false, lastReport(ThemePreference.System, day))
    }

    @Test
    fun forcedLight_underNight_reportsLight() {
        assertEquals(false, lastReport(ThemePreference.Light, night))
    }

    @Test
    fun forcedDark_underDay_reportsDark() {
        assertEquals(true, lastReport(ThemePreference.Dark, day))
    }

    @Test
    fun forcedAmoled_underDay_reportsDark() {
        assertEquals(true, lastReport(ThemePreference.Amoled, day))
    }

    @Test
    fun storeChange_reReportsTheNewTheme() {
        val reports = mutableListOf<Boolean>()
        val store = MutableStateFlow(UserSettings(theme = ThemePreference.Dark))
        showApp(store, day, reports)
        rule.runOnIdle { assertEquals(listOf(true), reports) }

        store.value = UserSettings(theme = ThemePreference.Light)

        rule.runOnIdle { assertEquals(listOf(true, false), reports) }
    }

    /** The last `onDarkTheme` value with [theme] stored, under a system [uiMode]; null when never reported. */
    private fun lastReport(
        theme: ThemePreference,
        uiMode: Int,
    ): Boolean? {
        val reports = mutableListOf<Boolean>()
        showApp(MutableStateFlow(UserSettings(theme = theme)), uiMode, reports)
        return rule.runOnIdle { reports.lastOrNull() }
    }

    private fun showApp(
        store: MutableStateFlow<UserSettings>,
        uiMode: Int,
        reports: MutableList<Boolean>,
    ) {
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { this.uiMode = uiMode }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                PocketCpApp(shellTestEntries, store, onDarkTheme = { reports += it })
            }
        }
    }
}
