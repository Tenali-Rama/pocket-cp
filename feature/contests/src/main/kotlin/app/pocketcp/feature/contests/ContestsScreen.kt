package app.pocketcp.feature.contests

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.EmptyState
import app.pocketcp.core.designsystem.component.ErrorState
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpIconButton
import app.pocketcp.core.designsystem.component.PcpSecondaryButton
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.ContestRow
import app.pocketcp.core.ui.RefreshErrorBanner
import java.time.Instant

/** Share of the width each pane takes on wide windows: filters 1 : list 2 : detail 2 (detail only when shown). */
private const val FILTER_PANE_WEIGHT = 1f
private const val LIST_PANE_WEIGHT = 2f
private const val DETAIL_PANE_WEIGHT = 2f

/**
 * Contests: a title bar with a refresh button, the filter chips and the list area for [state]'s status, plus the
 * [detail] of a contest. Compact width shows the opened contest as a page instead (back arrow and Back close it);
 * medium width shows chips above the list | detail; expanded shows filter pane | list | detail, with the shown
 * contest's row highlighted. Without a shown contest, wide windows show filter pane | list. [now] (from
 * `rememberNow()`) drives every countdown. Stateless, so screenshots pass a fixed state and time.
 */
@Composable
internal fun ContestsScreen(
    state: ContestsUiState,
    detail: ContestDetailUiState,
    now: Instant,
    actions: ContestsActions,
    detailActions: ContestDetailActions,
    modifier: Modifier = Modifier,
) {
    val screen = modifier.fillMaxSize().background(PocketCpTheme.colors.background)
    val width = LocalWindowLayout.current.widthClass
    if (width == WindowWidthClass.Compact && detail.selected != null) {
        BackHandler(onBack = detailActions::close)
        ContestDetailPage(detail, now, detailActions, onNavigateBack = detailActions::close, modifier = screen)
        return
    }
    Column(screen) {
        PcpTopAppBar(
            title = stringResource(R.string.contests_label),
            actions = {
                PcpIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.contests_refresh),
                    onClick = actions::refresh,
                )
            },
        )
        if (width == WindowWidthClass.Compact) {
            FilterRows(state.filter, actions)
            ListArea(state, now, actions, detailActions::open, highlighted = null, Modifier.weight(1f))
        } else {
            WidePanes(state, detail, now, actions, detailActions, Modifier.weight(1f))
        }
    }
}

/** Medium and wider: the filters and the list, and beside them the shown contest's detail when there is one. */
@Composable
private fun WidePanes(
    state: ContestsUiState,
    detail: ContestDetailUiState,
    now: Instant,
    actions: ContestsActions,
    detailActions: ContestDetailActions,
    modifier: Modifier = Modifier,
) {
    val shown = detail.shown
    Row(modifier) {
        if (shown != null && LocalWindowLayout.current.widthClass == WindowWidthClass.Medium) {
            Column(Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight()) {
                FilterRows(state.filter, actions)
                ListArea(state, now, actions, detailActions::open, shown, Modifier.weight(1f))
            }
        } else {
            FilterPane(state.filter, actions, Modifier.weight(FILTER_PANE_WEIGHT).fillMaxHeight())
            PaneDivider()
            val listModifier = Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight()
            ListArea(state, now, actions, detailActions::open, shown, listModifier)
        }
        if (shown != null) {
            PaneDivider()
            ContestDetailPane(detail, now, detailActions, Modifier.weight(DETAIL_PANE_WEIGHT).fillMaxHeight())
        }
    }
}

@Composable
private fun PaneDivider() = VerticalDivider(color = PocketCpTheme.colors.outline)

/**
 * The status-dependent part under the filters: skeleton, full error, empty state or the sections. A tapped row
 * calls [onOpen]; the [highlighted] row is the contest shown beside the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListArea(
    state: ContestsUiState,
    now: Instant,
    actions: ContestsActions,
    onOpen: (ContestId) -> Unit,
    highlighted: ContestId?,
    modifier: Modifier = Modifier,
) {
    val raised = PocketCpTheme.colors.surfaceRaised
    when (state.status) {
        ContestsStatus.Loading -> LoadingPlaceholder(modifier.fillMaxWidth())
        ContestsStatus.Error ->
            ErrorState(
                message = errorMessage(state.error ?: JudgeError.Unavailable()),
                onRetry = actions::refresh,
                modifier = modifier.fillMaxSize(),
            )
        ContestsStatus.Empty ->
            Column(modifier) {
                state.error?.let { Banner(it, actions) }
                EmptyContests(state.filter, actions, Modifier.weight(1f))
            }
        ContestsStatus.Loaded ->
            Column(modifier) {
                state.error?.let { Banner(it, actions) }
                PullToRefreshBox(
                    isRefreshing = state.userRefreshing,
                    onRefresh = actions::refresh,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        state.groups.forEach { group ->
                            item(key = "section_${group.section.name}", contentType = "section") {
                                SectionHeader(group.section.title)
                            }
                            items(group.contests, key = { it.id.value }, contentType = { "contest" }) { contest ->
                                ContestRow(
                                    contest = contest,
                                    now = now,
                                    onClick = { onOpen(contest.id) },
                                    modifier =
                                        if (highlighted == null) {
                                            Modifier
                                        } else {
                                            Modifier.highlighted(contest.id == highlighted, raised)
                                        },
                                )
                            }
                        }
                    }
                }
            }
    }
}

/** A row of a list shown beside the detail: `selected` for accessibility, and [raised] when it is the shown one. */
private fun Modifier.highlighted(
    isHighlighted: Boolean,
    raised: Color,
): Modifier = semantics { selected = isHighlighted }.then(if (isHighlighted) Modifier.background(raised) else Modifier)

@Composable
private fun Banner(
    error: JudgeError,
    actions: ContestsActions,
) {
    val spacing = PocketCpTheme.spacing
    RefreshErrorBanner(
        error = error,
        onRetry = actions::refresh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
    )
}

@Composable
private fun EmptyContests(
    filter: ContestFilter,
    actions: ContestsActions,
    modifier: Modifier = Modifier,
) {
    if (filter.isEmpty) {
        EmptyState(title = stringResource(R.string.contests_empty_title), modifier = modifier.fillMaxSize())
    } else {
        EmptyState(
            title = stringResource(R.string.contests_empty_filtered_title),
            message = stringResource(R.string.contests_empty_filtered_message),
            modifier = modifier.fillMaxSize(),
            action = {
                PcpSecondaryButton(
                    text = stringResource(R.string.contests_clear_filters),
                    onClick = actions::clearFilters,
                )
            },
        )
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
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.xs)
                .semantics { heading() },
    )
}

@get:StringRes
private val ContestSection.title: Int
    get() =
        when (this) {
            ContestSection.Running -> R.string.contests_section_running
            ContestSection.Upcoming -> R.string.contests_section_upcoming
            ContestSection.Past -> R.string.contests_section_past
        }

/** The full error state's message; a rejection shows the judge's own words. */
@Composable
internal fun errorMessage(error: JudgeError): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.contests_error_offline)
        is JudgeError.RateLimited -> stringResource(R.string.contests_error_rate_limited)
        is JudgeError.Unavailable -> stringResource(R.string.contests_error_unavailable)
        is JudgeError.Malformed -> stringResource(R.string.contests_error_malformed)
        is JudgeError.Rejected -> error.message.orEmpty()
    }
