package app.pocketcp.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.adaptive.DevicePosture
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowHeightClass
import app.pocketcp.core.adaptive.WindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.adaptive.navigationTypeOf
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.OptionalModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings navigation and writes, against a [FakeSettingsRepository] behind a real [SettingsViewModel]. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val repository = FakeSettingsRepository(UserSettings(disabledModules = setOf(OptionalModule.Ai)))

    @Test
    fun compact_listOpensAppearance_andBackReturnsToTheList() {
        show(WindowWidthClass.Compact, setOf(OptionalModule.Ai))
        rule.onNodeWithTag("settings_theme_light").assertDoesNotExist()

        rule.onNodeWithTag("settings_category_appearance").performClick()
        rule.onNodeWithTag("settings_theme_light").assertExists()
        rule.onNodeWithTag("settings_category_appearance").assertDoesNotExist()

        rule.onNodeWithContentDescription("Navigate back").performClick()
        rule.onNodeWithTag("settings_category_appearance").assertExists()

        rule.onNodeWithTag("settings_category_appearance").performClick()
        rule.onNodeWithTag("settings_theme_light").assertExists()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("settings_category_appearance").assertExists()
        assertFalse(rule.activity.isFinishing)
    }

    @Test
    fun features_absent_whenNoModuleIsInstalled() {
        show(WindowWidthClass.Compact, emptySet())

        rule.onNodeWithTag("settings_category_appearance").assertExists()
        rule.onNodeWithTag("settings_category_features").assertDoesNotExist()
    }

    @Test
    fun expanded_showsListAndAppearanceSideBySide() {
        show(WindowWidthClass.Expanded, setOf(OptionalModule.Ai))

        rule.onNodeWithTag("settings_category_appearance").assertIsSelected()
        rule.onNodeWithTag("settings_category_features").assertExists()
        rule.onNodeWithTag("settings_theme_system").assertIsSelected()
    }

    @Test
    fun appearanceChoices_writeToTheRepository() {
        show(WindowWidthClass.Expanded, emptySet())

        rule.onNodeWithTag("settings_theme_light").performClick()
        rule.onNodeWithTag("settings_theme_light").assertIsSelected()
        rule.onNodeWithTag("settings_code_ligatures").performScrollTo().performClick()
        rule.onNodeWithTag("settings_density_compact").performScrollTo().performClick()

        rule.runOnIdle {
            assertEquals(ThemePreference.Light, repository.stored.value.theme)
            assertFalse(repository.stored.value.codeLigatures)
            assertEquals(DensityPreference.Compact, repository.stored.value.density)
        }
    }

    @Test
    fun featureSwitch_turnsAModuleOffAndOn() {
        show(WindowWidthClass.Expanded, setOf(OptionalModule.Ai, OptionalModule.Stats))
        rule.onNodeWithTag("settings_category_features").performClick()
        rule.onNodeWithTag("settings_module_ai").assertIsOff()

        rule.onNodeWithTag("settings_module_stats").assertIsOn().performClick()
        rule.onNodeWithTag("settings_module_ai").performClick()

        rule.runOnIdle { assertEquals(setOf(OptionalModule.Stats), repository.stored.value.disabledModules) }
    }

    private fun show(
        width: WindowWidthClass,
        installed: Set<OptionalModule>,
    ) {
        val viewModel = SettingsViewModel(repository, installed)
        rule.setContent {
            Themed(width) {
                val state by viewModel.state.collectAsState()
                SettingsRoute(state, viewModel)
            }
        }
    }
}

@Composable
private fun Themed(
    width: WindowWidthClass,
    content: @Composable () -> Unit,
) {
    val layout = WindowLayout(width, WindowHeightClass.Medium, DevicePosture.Flat, navigationTypeOf(width))
    PocketCpTheme(ThemeMode.Dark) {
        CompositionLocalProvider(LocalWindowLayout provides layout, content = content)
    }
}
