package app.pocketcp.feature.contests

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.EmptyState
import app.pocketcp.core.designsystem.component.ErrorState
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.LocalClock
import app.pocketcp.core.ui.ProblemRow
import app.pocketcp.core.ui.RefreshErrorBanner
import app.pocketcp.core.ui.formatCountdown
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import app.pocketcp.core.ui.R as UiR

/** Test tag of the detail's scrolling content. */
internal const val CONTEST_DETAIL_TAG = "contest_detail"

private const val MINUTES_PER_HOUR = 60L

/** Compact width: the detail as a full page under a title bar with a back arrow. */
@Composable
internal fun ContestDetailPage(
    state: ContestDetailUiState,
    now: Instant,
    actions: ContestDetailActions,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PcpTopAppBar(title = stringResource(R.string.contest_detail_title), onNavigateBack = onNavigateBack)
        ContestDetailPane(state, now, actions, Modifier.weight(1f))
    }
}

/**
 * The detail of [state]'s shown contest: a header (name, division, type, phase and a countdown to [now] or its
 * date and length) above its problems, or the state of either part while it is missing.
 */
@Composable
internal fun ContestDetailPane(
    state: ContestDetailUiState,
    now: Instant,
    actions: ContestDetailActions,
    modifier: Modifier = Modifier,
) {
    val contest = state.contest
    when {
        state.status == DetailStatus.NotFound ->
            EmptyState(title = stringResource(R.string.contest_detail_not_found), modifier = modifier.fillMaxSize())
        contest == null -> LoadingPlaceholder(modifier.fillMaxWidth())
        else ->
            LazyColumn(modifier.fillMaxSize().testTag(CONTEST_DETAIL_TAG)) {
                item(key = "header", contentType = "header") { ContestHeader(contest, state.timing, now) }
                item(key = "problems_title", contentType = "title") { ProblemsTitle() }
                problems(state, actions)
            }
    }
}

/** The problems part for [state]'s [ProblemsStatus]; a refresh failure over cached problems is a banner. */
private fun LazyListScope.problems(
    state: ContestDetailUiState,
    actions: ContestDetailActions,
) {
    val stateModifier = Modifier.fillMaxWidth()
    when (state.problemsStatus) {
        ProblemsStatus.Loading -> item(key = "loading") { LoadingPlaceholder(stateModifier, rows = 3) }
        ProblemsStatus.Error ->
            item(key = "error") {
                ErrorState(
                    message = problemsErrorMessage(state.problemsError ?: JudgeError.Unavailable()),
                    onRetry = actions::retryProblems,
                    modifier = stateModifier,
                )
            }
        ProblemsStatus.NotYetAdded ->
            item(key = "pending") {
                EmptyState(title = stringResource(R.string.contest_detail_problems_pending), modifier = stateModifier)
            }
        ProblemsStatus.Empty ->
            item(key = "empty") {
                EmptyState(title = stringResource(R.string.contest_detail_no_problems), modifier = stateModifier)
            }
        ProblemsStatus.Loaded -> {
            state.problemsError?.let { error ->
                item(key = "banner", contentType = "banner") {
                    val spacing = PocketCpTheme.spacing
                    RefreshErrorBanner(
                        error = error,
                        onRetry = actions::retryProblems,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
                    )
                }
            }
            items(state.problems, key = { it.id.display }, contentType = { "problem" }) { problem ->
                // Opening a problem is M3.
                ProblemRow(
                    problem = problem,
                    hideTags = state.hideTags,
                    hideRating = state.hideRatings,
                    highlighted = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun ContestHeader(
    contest: Contest,
    timing: ContestTiming,
    now: Instant,
) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val typography = PocketCpTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = contest.name,
            style = typography.headline,
            color = colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = metaLine(contest), style = typography.body, color = colors.onSurfaceMuted)
        timingText(timing, now)?.let { Text(text = it, style = typography.title, color = colors.onBackground) }
    }
}

@Composable
private fun ProblemsTitle() {
    val spacing = PocketCpTheme.spacing
    Text(
        text = stringResource(R.string.contest_detail_problems),
        style = PocketCpTheme.typography.label,
        color = PocketCpTheme.colors.onSurfaceMuted,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.xs)
                .semantics { heading() },
    )
}

/** "Div. 2 · CF · Finished"; without the type when the judge names none. */
@Composable
private fun metaLine(contest: Contest): String {
    val division = stringResource(contest.division.label)
    val phase = stringResource(contest.phase.label)
    return if (contest.kind == ContestKind.Unknown) {
        stringResource(R.string.contest_detail_meta_no_kind, division, phase)
    } else {
        stringResource(R.string.contest_detail_meta, division, stringResource(contest.kind.label), phase)
    }
}

/** The countdown to [now], or the start date (in [LocalClock]'s zone) and length; `null` when there is none. */
@Composable
private fun timingText(
    timing: ContestTiming,
    now: Instant,
): String? =
    when (timing) {
        is ContestTiming.StartsIn ->
            stringResource(UiR.string.ui_contest_starts_in, formatCountdown(Duration.between(now, timing.start)))
        is ContestTiming.EndsIn ->
            stringResource(UiR.string.ui_contest_ends_in, formatCountdown(Duration.between(now, timing.end)))
        is ContestTiming.Held -> {
            val zone = LocalClock.current.zone
            val formatter =
                remember(zone) {
                    DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(Locale.getDefault())
                        .withZone(zone)
                }
            stringResource(R.string.contest_detail_held, formatter.format(timing.start), formatLength(timing.length))
        }
        ContestTiming.NotAnnounced -> stringResource(UiR.string.ui_contest_start_unknown)
        ContestTiming.None -> null
    }

/** A contest's length as hours and minutes, `02:15`. */
private fun formatLength(length: Duration): String =
    String.format(Locale.ROOT, "%02d:%02d", length.toHours(), length.toMinutes() % MINUTES_PER_HOUR)

@get:StringRes
private val ContestPhase.label: Int
    get() =
        when (this) {
            ContestPhase.Upcoming -> UiR.string.ui_contest_phase_upcoming
            ContestPhase.Running -> UiR.string.ui_contest_phase_running
            ContestPhase.Finished -> UiR.string.ui_contest_phase_finished
            ContestPhase.Unknown -> UiR.string.ui_contest_phase_unknown
        }

/** The problems' full error message; a rejection shows the judge's own words. */
@Composable
private fun problemsErrorMessage(error: JudgeError): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.contest_detail_problems_error_offline)
        else -> errorMessage(error)
    }
