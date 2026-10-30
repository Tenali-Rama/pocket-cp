package app.pocketcp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.model.Problem
import java.text.NumberFormat

/**
 * One problemset problem: its `display` id and name, its tags and solved count, and its rating as a chip in
 * the rating's rank colour. [hideTags] and [hideRating] drop those parts entirely, from the screen and from
 * what TalkBack reads (spec §7.3: no spoilers while practising). A [highlighted] row (e.g. the one open in the
 * detail pane) sits on `colors.surfaceRaised` and is announced as selected. Tagged `problem_<display>`.
 */
@Composable
fun ProblemRow(
    problem: Problem,
    hideTags: Boolean,
    hideRating: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val typography = PocketCpTheme.typography
    val display = problem.id.display
    val rating = problem.rating.takeUnless { hideRating }
    val tags = problem.tags.takeUnless { hideTags }.orEmpty()
    Row(
        modifier =
            modifier
                .testTag("problem_$display")
                .semantics(mergeDescendants = true) { selected = highlighted }
                .then(if (highlighted) Modifier.background(colors.surfaceRaised) else Modifier)
                .clickable(onClick = onClick)
                .heightIn(min = spacing.listItemTwoLineHeight)
                .padding(horizontal = spacing.lg, vertical = spacing.listItemVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = "$display · ${problem.name}",
                style = typography.body,
                color = colors.onSurface,
            )
            if (tags.isNotEmpty()) {
                Text(text = tags.joinToString(", "), style = typography.bodySmall, color = colors.onSurfaceMuted)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            if (rating != null) RatingChip(rating)
            problem.solvedCount?.let { count ->
                val formatted = NumberFormat.getIntegerInstance().format(count)
                Text(
                    text = stringResource(R.string.ui_problem_solved_count, formatted),
                    style = typography.bodySmall,
                    color = colors.onSurfaceMuted,
                )
            }
        }
    }
}

/** The rating in its rank colour inside a hairline outline of the same colour. */
@Composable
private fun RatingChip(rating: Int) {
    val color = PocketCpTheme.colors.rank.ofRating(rating)
    val spacing = PocketCpTheme.spacing
    Text(
        text = rating.toString(),
        style = PocketCpTheme.typography.label,
        color = color,
        modifier =
            Modifier
                .border(BorderStroke(Dp.Hairline, color), PocketCpTheme.shapes.small)
                .padding(horizontal = spacing.sm, vertical = spacing.xxs),
    )
}
