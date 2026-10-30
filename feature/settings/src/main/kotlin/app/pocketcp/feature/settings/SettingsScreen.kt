package app.pocketcp.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpListItem
import app.pocketcp.core.designsystem.component.PcpTopAppBar

/** Share of the width the category list takes next to the detail pane (list 2 : detail 3). */
private const val LIST_PANE_WEIGHT = 2f
private const val DETAIL_PANE_WEIGHT = 3f

/** [SettingsScreen] with the open category kept across rotation and process death. */
@Composable
internal fun SettingsRoute(
    state: SettingsUiState?,
    actions: SettingsActions,
) {
    var selected by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    SettingsScreen(state = state, actions = actions, selected = selected, onSelect = { selected = it })
}

/**
 * Settings: a category list and the [selected] category's page. Compact width shows one at a time (the page has a
 * back arrow and handles Back); wider windows show list | page, with Appearance when nothing is [selected].
 * A [selected] category that is no longer offered counts as none. `null` [state] is the first read: a skeleton.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState?,
    actions: SettingsActions,
    selected: SettingsCategory?,
    onSelect: (SettingsCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = modifier.fillMaxSize().background(PocketCpTheme.colors.background)
    if (state == null) {
        Column(screen) {
            PcpTopAppBar(title = stringResource(R.string.settings_label))
            LoadingPlaceholder()
        }
        return
    }
    val open = selected?.takeIf { it in state.categories }
    if (LocalWindowLayout.current.widthClass == WindowWidthClass.Compact) {
        if (open == null) {
            CategoryList(state.categories, highlighted = null, onSelect = onSelect, modifier = screen)
        } else {
            BackHandler { onSelect(null) }
            CategoryPage(open, state, actions, onNavigateBack = { onSelect(null) }, modifier = screen)
        }
    } else {
        val shown = open ?: SettingsCategory.Appearance
        Row(screen) {
            CategoryList(
                categories = state.categories,
                highlighted = shown,
                onSelect = onSelect,
                modifier = Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight(),
            )
            VerticalDivider(color = PocketCpTheme.colors.outline)
            CategoryPage(
                category = shown,
                state = state,
                actions = actions,
                onNavigateBack = null,
                modifier = Modifier.weight(DETAIL_PANE_WEIGHT).fillMaxHeight(),
            )
        }
    }
}

/** The categories under the screen title; the [highlighted] one is the page shown beside the list. */
@Composable
private fun CategoryList(
    categories: List<SettingsCategory>,
    highlighted: SettingsCategory?,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PcpTopAppBar(title = stringResource(R.string.settings_label))
        LazyColumn {
            items(categories, key = { it.name }) { category ->
                val isHighlighted = category == highlighted
                val highlight = if (isHighlighted) Modifier.background(PocketCpTheme.colors.surfaceRaised) else Modifier
                PcpListItem(
                    headline = stringResource(category.title),
                    supporting = stringResource(category.supporting),
                    onClick = { onSelect(category) },
                    modifier =
                        Modifier
                            .testTag(category.testTag)
                            .semantics { selected = isHighlighted }
                            .then(highlight),
                )
            }
        }
    }
}

/** One category's page under a title bar, with a back arrow when [onNavigateBack] is set. */
@Composable
private fun CategoryPage(
    category: SettingsCategory,
    state: SettingsUiState,
    actions: SettingsActions,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PcpTopAppBar(title = stringResource(category.title), onNavigateBack = onNavigateBack)
        when (category) {
            SettingsCategory.Appearance -> AppearanceSettings(state.settings, actions)
            SettingsCategory.Features ->
                FeaturesSettings(state.installedModules, state.settings.disabledModules, actions)
        }
    }
}

private val SettingsCategory.title: Int
    get() =
        when (this) {
            SettingsCategory.Appearance -> R.string.settings_category_appearance
            SettingsCategory.Features -> R.string.settings_category_features
        }

private val SettingsCategory.supporting: Int
    get() =
        when (this) {
            SettingsCategory.Appearance -> R.string.settings_category_appearance_supporting
            SettingsCategory.Features -> R.string.settings_category_features_supporting
        }

/** `settings_category_appearance`, `settings_category_features`: Maestro and tests select categories by these. */
internal val SettingsCategory.testTag: String
    get() = "settings_category_${name.lowercase()}"
