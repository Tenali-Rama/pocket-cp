package app.pocketcp.feature.problemset

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpFilterChip
import app.pocketcp.core.designsystem.component.PcpSecondaryButton
import app.pocketcp.core.designsystem.component.PcpTextButton
import app.pocketcp.core.model.ContestDivision
import kotlin.math.roundToInt

/** How much of the window the filter sheet may cover, and how much the scrim dims what it covers. */
private const val SHEET_MAX_HEIGHT_FRACTION = 0.85f
private const val SCRIM_ALPHA = 0.6f

/**
 * Compact and medium width: "Filters" (with the active-filter count; opens the sheet), tagged
 * `problemset_filters`, and "Random".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterAndRandomButtons(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = state.options.filter.activeCount
    val spacing = PocketCpTheme.spacing
    // Wraps in a narrow search pane (medium width).
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        PcpSecondaryButton(
            text =
                if (count == 0) {
                    stringResource(R.string.problemset_filters)
                } else {
                    stringResource(R.string.problemset_filters_count, count)
                },
            onClick = onOpenFilters,
            modifier = Modifier.testTag("problemset_filters"),
        )
        RandomButton(state, actions)
    }
}

/** "Random", tagged `problemset_random`: disabled while the list is empty. */
@Composable
internal fun RandomButton(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    PcpSecondaryButton(
        text = stringResource(R.string.problemset_random),
        onClick = actions::pickRandom,
        enabled = state.canPickRandom,
        modifier = modifier.testTag("problemset_random"),
    )
}

/**
 * Compact and medium width: the filters as a sheet from the bottom over a scrim, inside this window. Tapping the
 * scrim, Back or "Done" calls [onDismiss]; filters apply as they change.
 */
@Composable
internal fun FilterSheet(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PocketCpTheme.colors
    val corner = PocketCpTheme.shapes.extraLarge
    val sheetShape = corner.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0))
    val title = stringResource(R.string.problemset_filters)
    BackHandler(onBack = onDismiss)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = SCRIM_ALPHA))
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        FilterPanel(
            state = state,
            actions = actions,
            onDone = onDismiss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * SHEET_MAX_HEIGHT_FRACTION)
                    .clip(sheetShape)
                    .background(colors.surfaceRaised)
                    // Taps on the sheet itself never reach the scrim.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics { paneTitle = title }
                    .testTag("problemset_filter_sheet"),
        )
    }
}

/**
 * The filters: a header with "Clear" (and "Done" in the sheet), then rating range, divisions and tags. While tags
 * are hidden the tag chips sit behind "Show tag filter". Expanded width shows it as a side panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterPanel(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
) {
    val spacing = PocketCpTheme.spacing
    val filter = state.options.filter
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.sm, top = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.problemset_filters),
                style = PocketCpTheme.typography.title,
                color = PocketCpTheme.colors.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            PcpTextButton(
                text = stringResource(R.string.problemset_filters_clear),
                onClick = actions::clearFilters,
                enabled = filter.activeCount > 0,
                modifier = Modifier.testTag("problemset_filters_clear"),
            )
            if (onDone != null) {
                PcpTextButton(
                    text = stringResource(R.string.problemset_filters_done),
                    onClick = onDone,
                    modifier = Modifier.testTag("problemset_filters_done"),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            RatingFilter(filter, actions)
            GroupTitle(R.string.problemset_filter_division, Modifier.padding(top = spacing.md))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ContestDivision.entries.forEach { division ->
                    PcpFilterChip(
                        label = stringResource(division.label),
                        selected = division in filter.divisions,
                        onClick = { actions.toggleDivision(division) },
                        modifier = Modifier.testTag(division.testTag),
                    )
                }
            }
            GroupTitle(R.string.problemset_filter_tags, Modifier.padding(top = spacing.md))
            TagFilter(state, actions)
        }
    }
}

/** The rating range, 800–3500 in steps of 100, tagged `problemset_rating`. */
@Composable
private fun RatingFilter(
    filter: ProblemFilter,
    actions: ProblemsetActions,
) {
    val colors = PocketCpTheme.colors
    // The thumbs follow the finger at once; the filter takes each new step.
    var range by remember { mutableStateOf(filter.ratingMin.toFloat()..filter.ratingMax.toFloat()) }
    LaunchedEffect(filter.ratingMin, filter.ratingMax) {
        range = filter.ratingMin.toFloat()..filter.ratingMax.toFloat()
    }
    val shown =
        if (filter.ratingNarrowed) {
            stringResource(R.string.problemset_filter_rating_range, filter.ratingMin, filter.ratingMax)
        } else {
            stringResource(R.string.problemset_filter_rating_any)
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        GroupTitle(R.string.problemset_filter_rating, Modifier.weight(1f))
        Text(text = shown, style = PocketCpTheme.typography.body, color = colors.onSurface)
    }
    val label = stringResource(R.string.problemset_filter_rating)
    RangeSlider(
        value = range,
        onValueChange = { new ->
            range = new
            val min = new.start.roundToInt()
            val max = new.endInclusive.roundToInt()
            if (ProblemFilter.snapRange(min, max) != filter.ratingMin to filter.ratingMax) {
                actions.setRatingRange(min, max)
            }
        },
        valueRange = ProblemFilter.MIN_RATING.toFloat()..ProblemFilter.MAX_RATING.toFloat(),
        steps = (ProblemFilter.MAX_RATING - ProblemFilter.MIN_RATING) / ProblemFilter.RATING_STEP - 1,
        colors =
            SliderDefaults.colors(
                thumbColor = colors.onSurface,
                activeTrackColor = colors.onSurface,
                inactiveTrackColor = colors.outline,
                // Ticks in their track's colour: 27 steps would otherwise read as a dotted line.
                activeTickColor = colors.onSurface,
                inactiveTickColor = colors.outline,
            ),
        modifier =
            Modifier.fillMaxWidth().testTag("problemset_rating").semantics {
                contentDescription = label
                stateDescription = shown
            },
    )
}

/** Tag chips; while tags are hidden, collapsed behind "Show tag filter" (`problemset_show_tags`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilter(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
) {
    val selected = state.options.filter.tags
    var revealed by rememberSaveable { mutableStateOf(false) }
    if (state.options.hideTags && !revealed) {
        PcpTextButton(
            text =
                if (selected.isEmpty()) {
                    stringResource(R.string.problemset_filter_show_tags)
                } else {
                    stringResource(R.string.problemset_filter_show_tags_selected, selected.size)
                },
            onClick = { revealed = true },
            modifier = Modifier.testTag("problemset_show_tags"),
        )
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.sm)) {
        state.tags.forEach { tag ->
            PcpFilterChip(
                label = tag,
                selected = tag in selected,
                onClick = { actions.toggleTag(tag) },
                modifier = Modifier.testTag(tagTestTag(tag)),
            )
        }
    }
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

/** Maestro id of a tag chip: `problemset_tag_<tag>`, spaces as `_` (`problemset_tag_two_pointers`). */
internal fun tagTestTag(tag: String): String = "problemset_tag_${tag.replace(' ', '_')}"

/** Maestro id of a division chip, e.g. `problemset_division_div2`. Never rename one. */
internal val ContestDivision.testTag: String get() = "problemset_division_${name.lowercase()}"

@get:StringRes
private val ContestDivision.label: Int
    get() =
        when (this) {
            ContestDivision.Div1 -> R.string.problemset_division_div1
            ContestDivision.Div2 -> R.string.problemset_division_div2
            ContestDivision.Div3 -> R.string.problemset_division_div3
            ContestDivision.Div4 -> R.string.problemset_division_div4
            ContestDivision.Div1And2 -> R.string.problemset_division_div1and2
            ContestDivision.Educational -> R.string.problemset_division_educational
            ContestDivision.Global -> R.string.problemset_division_global
            ContestDivision.Other -> R.string.problemset_division_other
        }
