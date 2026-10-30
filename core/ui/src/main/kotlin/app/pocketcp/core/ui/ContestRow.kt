package app.pocketcp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpListItem
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestPhase
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One contest in a list: its name, when it starts or ends relative to [now] (from [rememberNow]),
 * and its phase as a text label. Upcoming and running contests count down; finished ones show their start
 * date (in [LocalClock]'s zone) and length. The whole row is one click target, tagged `contest_<id>`.
 */
@Composable
fun ContestRow(
    contest: Contest,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PcpListItem(
        headline = contest.name,
        supporting = contestTime(contest, now, LocalClock.current.zone),
        trailing = { PhaseLabel(contest.phase) },
        onClick = onClick,
        modifier = modifier.testTag("contest_${contest.id.value}"),
    )
}

/** The row's time line, or `null` when there is nothing to say (an [ContestPhase.Unknown] phase). */
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
