package app.pocketcp.feature.contests

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpFilterChip
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind

/** Compact width: one horizontally scrolling chip row per filter, above the list. */
@Composable
internal fun FilterRows(
    filter: ContestFilter,
    actions: ContestsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ChipRow { DivisionChips(filter, actions) }
        ChipRow { KindChips(filter, actions) }
    }
}

/** Medium and wider: the filters as a side pane, each group under its name, chips wrapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterPane(
    filter: ContestFilter,
    actions: ContestsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        GroupTitle(R.string.contests_filter_division)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) { DivisionChips(filter, actions) }
        GroupTitle(R.string.contests_filter_type, Modifier.padding(top = spacing.md))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) { KindChips(filter, actions) }
    }
}

@Composable
private fun ChipRow(chips: @Composable () -> Unit) {
    val spacing = PocketCpTheme.spacing
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) { chips() }
}

@Composable
private fun GroupTitle(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(title),
        style = PocketCpTheme.typography.label,
        color = PocketCpTheme.colors.onSurfaceMuted,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun DivisionChips(
    filter: ContestFilter,
    actions: ContestsActions,
) {
    ContestDivision.entries.forEach { division ->
        PcpFilterChip(
            label = stringResource(division.label),
            selected = division in filter.divisions,
            onClick = { actions.toggleDivision(division) },
            modifier = Modifier.testTag(division.testTag),
        )
    }
}

@Composable
private fun KindChips(
    filter: ContestFilter,
    actions: ContestsActions,
) {
    ContestFilter.KindChoices.forEach { kind ->
        PcpFilterChip(
            label = stringResource(kind.label),
            selected = kind in filter.kinds,
            onClick = { actions.toggleKind(kind) },
            modifier = Modifier.testTag(kind.testTag),
        )
    }
}

/** Maestro id of a division chip, e.g. `contests_division_div2`. Never rename one. */
internal val ContestDivision.testTag: String get() = "contests_division_${name.lowercase()}"

/** Maestro id of a type chip, e.g. `contests_kind_cf`. Never rename one. */
internal val ContestKind.testTag: String get() = "contests_kind_${name.lowercase()}"

@get:StringRes
private val ContestDivision.label: Int
    get() =
        when (this) {
            ContestDivision.Div1 -> R.string.contests_division_div1
            ContestDivision.Div2 -> R.string.contests_division_div2
            ContestDivision.Div3 -> R.string.contests_division_div3
            ContestDivision.Div4 -> R.string.contests_division_div4
            ContestDivision.Div1And2 -> R.string.contests_division_div1and2
            ContestDivision.Educational -> R.string.contests_division_educational
            ContestDivision.Global -> R.string.contests_division_global
            ContestDivision.Other -> R.string.contests_division_other
        }

@get:StringRes
private val ContestKind.label: Int
    get() =
        when (this) {
            ContestKind.Cf -> R.string.contests_kind_cf
            ContestKind.Icpc -> R.string.contests_kind_icpc
            ContestKind.Ioi -> R.string.contests_kind_ioi
            // Never offered as a chip (ContestFilter.KindChoices).
            ContestKind.Unknown -> R.string.contests_division_other
        }
