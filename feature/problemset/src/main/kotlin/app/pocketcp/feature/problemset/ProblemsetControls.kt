package app.pocketcp.feature.problemset

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpChoiceRow
import app.pocketcp.core.designsystem.component.PcpFilterChip
import app.pocketcp.core.designsystem.component.PcpIconButton

/**
 * Search by name or id (`1900A`). Tagged `problemset_search`, its clear button `problemset_search_clear`. The
 * list follows each keystroke, so the IME's search key just closes the keyboard.
 */
@Composable
internal fun SearchField(
    text: String,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    val colors = PocketCpTheme.colors
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = actions::search,
        modifier = modifier.testTag("problemset_search"),
        textStyle = PocketCpTheme.typography.body,
        placeholder = { Text(stringResource(R.string.problemset_search_hint), style = PocketCpTheme.typography.body) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon =
            if (text.isEmpty()) {
                null
            } else {
                {
                    PcpIconButton(
                        icon = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.problemset_clear_search),
                        onClick = { actions.search("") },
                        modifier = Modifier.testTag("problemset_search_clear"),
                    )
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = PocketCpTheme.shapes.medium,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                focusedBorderColor = colors.onSurface,
                unfocusedBorderColor = colors.outline,
                cursorColor = colors.onSurface,
                focusedPlaceholderColor = colors.onSurfaceMuted,
                unfocusedPlaceholderColor = colors.onSurfaceMuted,
                focusedLeadingIconColor = colors.onSurfaceMuted,
                unfocusedLeadingIconColor = colors.onSurfaceMuted,
                focusedTrailingIconColor = colors.onSurfaceMuted,
                unfocusedTrailingIconColor = colors.onSurfaceMuted,
            ),
    )
}

/** Compact width: the offered orders as one horizontally scrolling chip row. */
@Composable
internal fun SortChips(
    options: ProblemsetOptions,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.sortChoices.forEach { sort ->
            PcpFilterChip(
                label = stringResource(sort.label),
                selected = sort == options.effectiveSort,
                onClick = { actions.sortBy(sort) },
                modifier = Modifier.testTag(sort.testTag),
            )
        }
    }
}

/** Medium and wider: the search field and the offered orders as a side pane. */
@Composable
internal fun SearchPane(
    searchText: String,
    options: ProblemsetOptions,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing.lg)) {
        SearchField(searchText, actions, Modifier.fillMaxWidth().padding(horizontal = spacing.lg))
        Text(
            text = stringResource(R.string.problemset_sort_title),
            style = PocketCpTheme.typography.label,
            color = PocketCpTheme.colors.onSurfaceMuted,
            modifier =
                Modifier
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.xs)
                    .semantics { heading() },
        )
        Column(Modifier.selectableGroup()) {
            options.sortChoices.forEach { sort ->
                PcpChoiceRow(
                    title = stringResource(sort.label),
                    selected = sort == options.effectiveSort,
                    onClick = { actions.sortBy(sort) },
                    modifier = Modifier.testTag(sort.testTag),
                )
            }
        }
    }
}

/** The top bar's overflow menu: "Hide tags" and "Hide ratings" (spec §7.3), each closing the menu. */
@Composable
internal fun SpoilerMenu(
    options: ProblemsetOptions,
    actions: ProblemsetActions,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PcpIconButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.problemset_more),
            onClick = { expanded = true },
            modifier = Modifier.testTag("problemset_menu"),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PocketCpTheme.colors.surfaceRaised,
        ) {
            ToggleItem(R.string.problemset_hide_tags, options.hideTags, "problemset_hide_tags") {
                actions.setHideTags(it)
                expanded = false
            }
            ToggleItem(R.string.problemset_hide_ratings, options.hideRatings, "problemset_hide_ratings") {
                actions.setHideRatings(it)
                expanded = false
            }
        }
    }
}

@Composable
private fun ToggleItem(
    @StringRes label: Int,
    checked: Boolean,
    testTag: String,
    onToggle: (Boolean) -> Unit,
) {
    val colors = PocketCpTheme.colors
    DropdownMenuItem(
        text = { Text(stringResource(label), style = PocketCpTheme.typography.body, color = colors.onSurface) },
        onClick = { onToggle(!checked) },
        modifier =
            Modifier.testTag(testTag).semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(checked)
            },
        trailingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.onSurfaceMuted,
                        checkmarkColor = colors.onPrimary,
                    ),
            )
        },
    )
}

/** Maestro id of a sort choice, e.g. `problemset_sort_newest`. Never rename one. */
internal val ProblemSort.testTag: String get() = "problemset_sort_${name.lowercase()}"

@get:StringRes
private val ProblemSort.label: Int
    get() =
        when (this) {
            ProblemSort.Newest -> R.string.problemset_sort_newest
            ProblemSort.RatingAscending -> R.string.problemset_sort_rating_ascending
            ProblemSort.RatingDescending -> R.string.problemset_sort_rating_descending
            ProblemSort.MostSolved -> R.string.problemset_sort_most_solved
            ProblemSort.Name -> R.string.problemset_sort_name
        }
