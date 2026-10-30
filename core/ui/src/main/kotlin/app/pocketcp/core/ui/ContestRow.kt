package app.pocketcp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestPhase
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One contest in a list: its name (wrapped, never cut), then its phase as a text label and when it starts
 * or ends relative to [now] (from [rememberNow]). Upcoming and running contests count down; finished ones
 * show their start date (in [LocalClock]'s zone) and length. The whole row is one click target, tagged
 * `contest_<id>`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContestRow(
    contest: Contest,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val typography = PocketCpTheme.typography
    val time = contestTime(contest, now, LocalClock.current.zone)
    Column(
        modifier =
            modifier
                .testTag("contest_${contest.id.value}")
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .heightIn(min = spacing.listItemTwoLineHeight)
                .padding(horizontal = spacing.lg, vertical = spacing.listItemVertical),
        verticalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterVertically),
    ) {
        Text(text = contest.name, style = typography.body, color = colors.onSurface)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            PhaseLabel(contest.phase)
            if (time != null) Text(text = time, style = typography.bodySmall, color = colors.onSurfaceMuted)
        }
    }
}

/** The row's time text, or `null` when there is nothing to say (an [ContestPhase.Unknown] phase). */
@Composable
private fun contestTime(
    contest: Contest,
    now: Instant,
    zone: ZoneId,
): String? {
    val start = contest.start
    return when {
        contest.phase == ContestPhase.Unknown -> null
        start == null -> stringResource(R.string.ui_contest_start_unknown)
        contest.phase == ContestPhase.Upcoming ->
            stringResource(R.string.ui_contest_starts_in, formatCountdown(Duration.between(now, start)))
        contest.phase == ContestPhase.Running ->
            stringResource(
                R.string.ui_contest_ends_in,
                formatCountdown(Duration.between(now, start.plus(contest.duration))),
            )
        else -> {
            val formatter =
                remember(zone) {
                    DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(Locale.getDefault())
                        .withZone(zone)
                }
            stringResource(R.string.ui_contest_finished_time, formatter.format(start), formatLength(contest.duration))
        }
    }
}

/** The phase as words, so it never depends on colour; a running contest's label is filled to stand out. */
@Composable
private fun PhaseLabel(phase: ContestPhase) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val shape = PocketCpTheme.shapes.small
    val running = phase == ContestPhase.Running
    val text =
        stringResource(
            when (phase) {
                ContestPhase.Upcoming -> R.string.ui_contest_phase_upcoming
                ContestPhase.Running -> R.string.ui_contest_phase_running
                ContestPhase.Finished -> R.string.ui_contest_phase_finished
                ContestPhase.Unknown -> R.string.ui_contest_phase_unknown
            },
        )
    val container =
        if (running) {
            Modifier.background(colors.primary, shape)
        } else {
            Modifier.border(BorderStroke(Dp.Hairline, colors.outline), shape)
        }
    Text(
        text = text,
        style = PocketCpTheme.typography.label,
        color = if (running) colors.onPrimary else colors.onSurfaceMuted,
        modifier = container.padding(horizontal = spacing.sm, vertical = spacing.xxs),
    )
}
