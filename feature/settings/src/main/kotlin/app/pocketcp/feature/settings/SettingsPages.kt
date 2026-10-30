package app.pocketcp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.StoredPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpChoiceRow
import app.pocketcp.core.designsystem.component.PcpSwitchRow
import app.pocketcp.core.model.OptionalModule

/** Theme, app and code text size, ligatures and density; every change goes straight to [actions]. */
@Composable
internal fun AppearanceSettings(
    settings: UserSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ChoiceGroup(
            R.string.settings_theme,
            "theme",
            ThemePreference.entries,
            settings.theme,
            actions::setTheme,
        ) { it.label }
        ChoiceGroup(
            R.string.settings_app_text_size,
            "app_text_size",
            TextSizePreference.entries,
            settings.appTextSize,
            actions::setAppTextSize,
        ) { it.label }
        ChoiceGroup(
            R.string.settings_code_text_size,
            "code_text_size",
            TextSizePreference.entries,
            settings.codeTextSize,
            actions::setCodeTextSize,
        ) { it.label }
        SectionHeader(R.string.settings_code)
        PcpSwitchRow(
            title = stringResource(R.string.settings_code_ligatures),
            supporting = stringResource(R.string.settings_code_ligatures_supporting),
            checked = settings.codeLigatures,
            onCheckedChange = actions::setCodeLigatures,
            modifier = Modifier.testTag("settings_code_ligatures"),
        )
        ChoiceGroup(
            R.string.settings_density,
            "density",
            DensityPreference.entries,
            settings.density,
            actions::setDensity,
        ) { it.label }
    }
}

/** One switch per installed module, on unless it is in [disabledModules]. */
@Composable
internal fun FeaturesSettings(
    modules: List<OptionalModule>,
    disabledModules: Set<OptionalModule>,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.settings_features_intro),
                style = PocketCpTheme.typography.bodySmall,
                color = PocketCpTheme.colors.onSurfaceMuted,
                modifier =
                    Modifier.padding(
                        horizontal = PocketCpTheme.spacing.lg,
                        vertical = PocketCpTheme.spacing.sm,
                    ),
            )
        }
        items(modules, key = { it.key }) { module ->
            PcpSwitchRow(
                title = stringResource(module.title),
                supporting = stringResource(module.supporting),
                checked = module !in disabledModules,
                onCheckedChange = { actions.setModuleEnabled(module, it) },
                modifier = Modifier.testTag("settings_module_${module.key}"),
            )
        }
    }
}

/** A titled single-choice group; option rows are tagged `settings_<group>_<key>` (e.g. `settings_theme_light`). */
@Composable
private fun <T : StoredPreference> ChoiceGroup(
    @StringRes title: Int,
    group: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> Int,
) {
    SectionHeader(title)
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            PcpChoiceRow(
                title = stringResource(label(option)),
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.testTag("settings_${group}_${option.key}"),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    @StringRes title: Int,
) {
    val spacing = PocketCpTheme.spacing
    Text(
        text = stringResource(title),
        style = PocketCpTheme.typography.label,
        color = PocketCpTheme.colors.onSurfaceMuted,
        modifier =
            Modifier
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.xs)
                .semantics { heading() },
    )
}

private val ThemePreference.label: Int
    get() =
        when (this) {
            ThemePreference.System -> R.string.settings_theme_system
            ThemePreference.Dark -> R.string.settings_theme_dark
            ThemePreference.Light -> R.string.settings_theme_light
            ThemePreference.Amoled -> R.string.settings_theme_amoled
        }

private val TextSizePreference.label: Int
    get() =
        when (this) {
            TextSizePreference.Small -> R.string.settings_text_size_small
            TextSizePreference.Default -> R.string.settings_text_size_default
            TextSizePreference.Large -> R.string.settings_text_size_large
            TextSizePreference.Largest -> R.string.settings_text_size_largest
        }

private val DensityPreference.label: Int
    get() =
        when (this) {
            DensityPreference.Comfortable -> R.string.settings_density_comfortable
            DensityPreference.Compact -> R.string.settings_density_compact
        }

private val OptionalModule.title: Int
    get() =
        when (this) {
            OptionalModule.LocalCompute -> R.string.settings_module_local_compute
            OptionalModule.Ai -> R.string.settings_module_ai
            OptionalModule.StressTester -> R.string.settings_module_stress_tester
            OptionalModule.PracticeAndStreaks -> R.string.settings_module_practice_streaks
            OptionalModule.Stats -> R.string.settings_module_stats
            OptionalModule.OtherJudgesCalendar -> R.string.settings_module_other_judges_calendar
            OptionalModule.FocusModeAutoStart -> R.string.settings_module_focus_mode_auto_start
        }

private val OptionalModule.supporting: Int
    get() =
        when (this) {
            OptionalModule.LocalCompute -> R.string.settings_module_local_compute_supporting
            OptionalModule.Ai -> R.string.settings_module_ai_supporting
            OptionalModule.StressTester -> R.string.settings_module_stress_tester_supporting
            OptionalModule.PracticeAndStreaks -> R.string.settings_module_practice_streaks_supporting
            OptionalModule.Stats -> R.string.settings_module_stats_supporting
            OptionalModule.OtherJudgesCalendar -> R.string.settings_module_other_judges_calendar_supporting
            OptionalModule.FocusModeAutoStart -> R.string.settings_module_focus_mode_auto_start_supporting
        }
