package app.pocketcp.shell

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpSpacing
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.designsystem.UiDensity
import app.pocketcp.core.navigation.FeatureEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The shell's theme: the stored preference over the system night mode, and the stored text and code settings. */
@RunWith(AndroidJUnit4::class)
class ShellThemeTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val night = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES
    private val day = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO
    private val undefined = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_UNDEFINED

    @Test
    fun system_followsNightMode_andIsDarkWhenUnspecified() {
        assertEquals(ThemeMode.Dark, themeModeOf(ThemePreference.System, night))
        assertEquals(ThemeMode.Light, themeModeOf(ThemePreference.System, day))
        assertEquals(ThemeMode.Dark, themeModeOf(ThemePreference.System, undefined))
        assertEquals(ThemeMode.Dark, themeModeOf(ThemePreference.System, Configuration.UI_MODE_TYPE_UNDEFINED))
    }

    @Test
    fun dark_isForced_whateverTheNightMode() {
        listOf(night, day, undefined).forEach { assertEquals(ThemeMode.Dark, themeModeOf(ThemePreference.Dark, it)) }
    }

    @Test
    fun light_isForced_whateverTheNightMode() {
        listOf(night, day, undefined).forEach { assertEquals(ThemeMode.Light, themeModeOf(ThemePreference.Light, it)) }
    }

    @Test
    fun amoled_isForced_whateverTheNightMode() {
        listOf(night, day, undefined).forEach {
            assertEquals(ThemeMode.Amoled, themeModeOf(ThemePreference.Amoled, it))
        }
    }

    @Test
    fun textSizes_scaleAsSettingsOffers() {
        val scales = TextSizePreference.entries.associateWith { it.scale }

        assertEquals(
            mapOf(
                TextSizePreference.Small to 0.9f,
                TextSizePreference.Default to 1f,
                TextSizePreference.Large to 1.15f,
                TextSizePreference.Largest to 1.3f,
            ),
            scales,
        )
    }

    @Test
    fun ligaturesOff_disablesCaltAndLiga_inTheShellsCodeStyle() {
        val probe = CodeStyleProbe()
        rule.setContent { PocketCpApp(listOf(probe), MutableStateFlow(UserSettings(codeLigatures = false))) }

        rule.runOnIdle { assertEquals("\"calt\" 0, \"liga\" 0", probe.fontFeatures) }
    }

    @Test
    fun ligaturesOn_leavesFontFeaturesUnset() {
        val probe = CodeStyleProbe()
        rule.setContent { PocketCpApp(listOf(probe), MutableStateFlow(UserSettings(codeLigatures = true))) }

        rule.runOnIdle { assertNull(probe.fontFeatures) }
    }

    @Test
    fun compactDensity_isTheShellsSpacing() {
        val probe = SpacingProbe()
        val settings = MutableStateFlow(UserSettings(density = DensityPreference.Compact))
        rule.setContent { PocketCpApp(listOf(probe), settings) }

        rule.runOnIdle { assertEquals(PocketCpSpacing.of(UiDensity.Compact), probe.spacing) }
    }

    @Test
    fun comfortableDensity_isTheShellsSpacing_afterCompact() {
        val probe = SpacingProbe()
        val settings = MutableStateFlow(UserSettings(density = DensityPreference.Compact))
        rule.setContent { PocketCpApp(listOf(probe), settings) }

        settings.value = UserSettings(density = DensityPreference.Comfortable)

        rule.runOnIdle { assertEquals(PocketCpSpacing.of(UiDensity.Comfortable), probe.spacing) }
    }

    @Test
    fun storeChange_recomposesTheShell_withoutRecreatingTheActivity() {
        val probe = CodeStyleProbe()
        val store = MutableStateFlow(UserSettings(codeLigatures = true))
        rule.setContent { PocketCpApp(listOf(probe), store) }
        val activity = rule.activity

        store.value = UserSettings(codeLigatures = false)

        rule.runOnIdle {
            assertEquals("\"calt\" 0, \"liga\" 0", probe.fontFeatures)
            assertEquals(activity, rule.activity)
        }
    }

    @Test
    fun firstFrame_waitsForTheFirstSettings() {
        val store = MutableSharedFlow<UserSettings>(replay = 1)
        rule.setContent { PocketCpApp(shellTestEntries, store) }

        rule.onNodeWithTag(ROOT_TAG).assertDoesNotExist()

        store.tryEmit(UserSettings(theme = ThemePreference.Light))
        rule.onNodeWithTag(ROOT_TAG).assertExists()
    }

    /** A single destination whose screen records the code style's font features as the shell themes it. */
    private class CodeStyleProbe : FeatureEntry {
        var fontFeatures: String? = "not composed"

        override val destination = shellTestEntries.first().destination

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) { fontFeatures = PocketCpTheme.typography.code.fontFeatureSettings }
        }
    }

    /** A single destination whose screen records the spacing tokens as the shell themes it. */
    private class SpacingProbe : FeatureEntry {
        var spacing: PocketCpSpacing? = null

        override val destination = shellTestEntries.first().destination

        override fun register(graph: NavGraphBuilder) {
            graph.composable(destination.route) { spacing = PocketCpTheme.spacing }
        }
    }
}
