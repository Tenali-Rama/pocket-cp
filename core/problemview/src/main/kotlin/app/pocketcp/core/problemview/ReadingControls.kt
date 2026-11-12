package app.pocketcp.core.problemview

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpIconButton
import app.pocketcp.core.model.StatementLanguage

/** Test tag of the reading menu's button. */
const val READING_MENU_TAG = "problem_reading"

/** The statement's maximum width for this preference: a reading-width token, or the whole pane for `Full`. */
@Composable
internal fun StatementWidthPreference.maxWidth(): Dp =
    when (this) {
        StatementWidthPreference.Narrow -> PocketCpTheme.spacing.readingWidthNarrow
        StatementWidthPreference.Comfortable -> PocketCpTheme.spacing.readingWidthComfortable
        StatementWidthPreference.Full -> Dp.Infinity
    }

/** EN | RU, one selected; each option is tagged `problem_language_<key>` and activated by tap or `Enter`. */
@Composable
internal fun LanguageToggle(
    language: StatementLanguage,
    onSelect: (StatementLanguage) -> Unit,
) {
    Row(Modifier.selectableGroup()) {
        StatementLanguage.entries.forEach { option ->
            LanguageOption(option, selected = option == language, onClick = { onSelect(option) })
        }
    }
}

@Composable
private fun LanguageOption(
    language: StatementLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val description = stringResource(language.description)
    val highlight = if (selected) Modifier.background(colors.surfaceRaised) else Modifier
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = spacing.touchTarget, minHeight = spacing.touchTarget)
                .clip(PocketCpTheme.shapes.medium)
                .then(highlight)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .semantics { contentDescription = description }
                .testTag("problem_language_${language.key}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(language.label),
            style = PocketCpTheme.typography.label,
            color = if (selected) colors.onBackground else colors.onSurfaceMuted,
        )
    }
}

/** The reading menu: text sizes, then reading widths, each a choice that closes the menu. */
@Composable
internal fun ReadingMenu(
    textSize: TextSizePreference,
    width: StatementWidthPreference,
    actions: ProblemActions,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        PcpIconButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.problemview_reading),
            onClick = { expanded = true },
            modifier = Modifier.testTag(READING_MENU_TAG),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PocketCpTheme.colors.surfaceRaised,
        ) {
            MenuHeading(R.string.problemview_text_size)
            TextSizePreference.entries.forEach { size ->
                ChoiceItem(size.label, size == textSize, "problem_text_${size.key}") {
                    actions.setTextSize(size)
                    expanded = false
                }
            }
            HorizontalDivider(color = PocketCpTheme.colors.outline)
            MenuHeading(R.string.problemview_width)
            StatementWidthPreference.entries.forEach { option ->
                ChoiceItem(option.label, option == width, "problem_width_${option.key}") {
                    actions.setWidth(option)
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun MenuHeading(
    @StringRes label: Int,
) {
    val spacing = PocketCpTheme.spacing
    Text(
        text = stringResource(label),
        style = PocketCpTheme.typography.label,
        color = PocketCpTheme.colors.onSurfaceMuted,
        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm).semantics { heading() },
    )
}

@Composable
private fun ChoiceItem(
    @StringRes label: Int,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = PocketCpTheme.colors
    DropdownMenuItem(
        text = { Text(stringResource(label), style = PocketCpTheme.typography.body, color = colors.onSurface) },
        onClick = onClick,
        modifier =
            Modifier.testTag(testTag).semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        trailingIcon = {
            // The row's selected state is what TalkBack reads; the mark only shows it.
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSurface)
        },
    )
}

@get:StringRes
private val StatementLanguage.label: Int
    get() =
        when (this) {
            StatementLanguage.En -> R.string.problemview_language_en
            StatementLanguage.Ru -> R.string.problemview_language_ru
        }

@get:StringRes
private val StatementLanguage.description: Int
    get() =
        when (this) {
            StatementLanguage.En -> R.string.problemview_language_en_description
            StatementLanguage.Ru -> R.string.problemview_language_ru_description
        }

@get:StringRes
private val TextSizePreference.label: Int
    get() =
        when (this) {
            TextSizePreference.Small -> R.string.problemview_text_small
            TextSizePreference.Default -> R.string.problemview_text_default
            TextSizePreference.Large -> R.string.problemview_text_large
            TextSizePreference.Largest -> R.string.problemview_text_largest
        }

@get:StringRes
private val StatementWidthPreference.label: Int
    get() =
        when (this) {
            StatementWidthPreference.Narrow -> R.string.problemview_width_narrow
            StatementWidthPreference.Comfortable -> R.string.problemview_width_comfortable
            StatementWidthPreference.Full -> R.string.problemview_width_full
        }
