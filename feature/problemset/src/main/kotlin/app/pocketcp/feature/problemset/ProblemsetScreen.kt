package app.pocketcp.feature.problemset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.EmptyState
import app.pocketcp.core.designsystem.component.ErrorState
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpIconButton
import app.pocketcp.core.designsystem.component.PcpSecondaryButton
import app.pocketcp.core.designsystem.component.PcpTextButton
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.ui.ProblemRow
import app.pocketcp.core.ui.RefreshErrorBanner

/** Shares of the width on wide windows: list 44 : statement 56, so the statement keeps at least 55 %. */
private const val LIST_PANE_WEIGHT = 0.44f
private const val DETAIL_PANE_WEIGHT = 0.56f

/**
 * The problem view for an id: `ProblemPane` on its own view model in the app, a fixed state in screenshots. The
 * `onBack` argument shows a back arrow (the compact page); `null` in the detail pane.
 */
internal typealias ProblemPaneSlot = @Composable (id: ProblemId, onBack: (() -> Unit)?, modifier: Modifier) -> Unit

/**
 * Problemset: the list column (a title bar with refresh and the spoiler toggles, the search field, "Filters",
 * "Random", the sort chips and the list area for [state]'s status) and the opened problem from [problemPane].
 * Compact width shows the opened problem as a page instead of the list (back arrow and Back close it); medium and
 * wider show list | statement with [ProblemsetUiState.shown] highlighted, or "Select a problem" with nothing to
 * show. The filters are a sheet over the screen ([filtersOpen] at first). [searchText] is the field's text, which
 * leads [state]'s query by the time a search takes. Stateless, so screenshots pass a state.
 */
@Composable
internal fun ProblemsetScreen(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    problemPane: ProblemPaneSlot,
    modifier: Modifier = Modifier,
    filtersOpen: Boolean = false,
) {
    val compact = LocalWindowLayout.current.widthClass == WindowWidthClass.Compact
    var sheetOpen by rememberSaveable { mutableStateOf(filtersOpen) }
    val openFilters = { sheetOpen = true }
    // Hoisted, so the list keeps its position while a compact page covers it.
    val listState = rememberLazyListState()
    val opened = state.opened
    Box(modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        if (compact && opened != null) {
            BackHandler(onBack = actions::close)
            problemPane(opened, actions::close, Modifier.fillMaxSize())
        } else {
            if (compact) {
                ListColumn(state, searchText, actions, listState, openFilters, null, Modifier.fillMaxSize())
            } else {
                WidePanes(state, searchText, actions, listState, openFilters, problemPane)
            }
            if (sheetOpen) FilterSheet(state, actions, onDismiss = { sheetOpen = false })
        }
    }
}

/**
 * Medium and wider: the list column | the shown problem (at least 55 % of the width), or "Select a problem" when
 * there is none to show. No back arrow: the list is already visible (ADR 0016).
 */
@Composable
private fun WidePanes(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    listState: LazyListState,
    onOpenFilters: () -> Unit,
    problemPane: ProblemPaneSlot,
) {
    val shown = state.shown
    Row(Modifier.fillMaxSize()) {
        val list = Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight()
        ListColumn(state, searchText, actions, listState, onOpenFilters, shown, list)
        VerticalDivider(color = PocketCpTheme.colors.outline)
        val detail = Modifier.weight(DETAIL_PANE_WEIGHT).fillMaxHeight()
        if (shown != null) {
            problemPane(shown, null, detail)
        } else {
            EmptyState(title = stringResource(R.string.problemset_select_problem), modifier = detail)
        }
    }
}

/** The title bar, the search field, "Filters" and "Random", the sort chips and the list area. */
@Composable
private fun ListColumn(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    listState: LazyListState,
    onOpenFilters: () -> Unit,
    highlighted: ProblemId?,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(modifier) {
        PcpTopAppBar(
            title = stringResource(R.string.problemset_label),
            actions = {
                PcpIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.problemset_refresh),
                    onClick = actions::refresh,
                )
                SpoilerMenu(state.options, actions)
            },
        )
        SearchField(searchText, actions, Modifier.fillMaxWidth().padding(horizontal = spacing.lg))
        FilterAndRandomButtons(
            state,
            actions,
            onOpenFilters,
            Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm),
        )
        SortChips(state.options, actions, Modifier.padding(vertical = spacing.xs))
        ListArea(state, actions, listState, onOpenFilters, highlighted, Modifier.weight(1f))
    }
}

/**
 * The status-dependent part: skeleton, full error, empty state or the problems in [listState]. A tapped row opens
 * its problem; the [highlighted] row is the one shown beside the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListArea(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    listState: LazyListState,
    onOpenFilters: () -> Unit,
    highlighted: ProblemId?,
    modifier: Modifier = Modifier,
) {
    when (state.status) {
        ProblemsetStatus.Loading -> LoadingPlaceholder(modifier.fillMaxWidth())
        ProblemsetStatus.Error ->
            ErrorState(
                message = errorMessage(state.error ?: JudgeError.Unavailable()),
                onRetry = actions::refresh,
                modifier = modifier.fillMaxSize(),
            )
        ProblemsetStatus.Empty ->
            Column(modifier) {
                state.error?.let { Banner(it, actions) }
                EmptyProblems(state.options, actions, onOpenFilters, Modifier.weight(1f))
            }
        ProblemsetStatus.Loaded ->
            Column(modifier) {
                state.error?.let { Banner(it, actions) }
                PullToRefreshBox(
                    isRefreshing = state.userRefreshing,
                    onRefresh = actions::refresh,
                    modifier = Modifier.weight(1f),
                ) {
                    ProblemList(state, actions, listState, highlighted)
                }
            }
    }
}

@Composable
private fun ProblemList(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    listState: LazyListState,
    highlighted: ProblemId?,
) {
    val options = state.options
    // A new search, filter or order starts at its first result; recreating the screen (rotation) keeps the
    // position. A new random pick scrolls to its problem.
    val shownFor = "${options.query}\u0000${options.effectiveSort}\u0000${options.filter}"
    var lastShownFor by rememberSaveable { mutableStateOf(shownFor) }
    LaunchedEffect(shownFor) {
        if (shownFor != lastShownFor) {
            lastShownFor = shownFor
            listState.scrollToItem(0)
        }
    }
    val pick = state.pick
    var lastPick by rememberSaveable { mutableIntStateOf(pick.count) }
    LaunchedEffect(pick) {
        if (pick.count != lastPick) {
            lastPick = pick.count
            val index = state.problems.indexOfFirst { it.id.display == pick.display }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        items(state.problems, key = { it.id.display }, contentType = { "problem" }) { problem ->
            ProblemRow(
                problem = problem,
                hideTags = options.hideTags,
                hideRating = options.hideRatings,
                highlighted = problem.id == highlighted || problem.id.display == pick.display,
                onClick = { actions.open(problem.id) },
            )
        }
    }
}

@Composable
private fun Banner(
    error: JudgeError,
    actions: ProblemsetActions,
) {
    val spacing = PocketCpTheme.spacing
    RefreshErrorBanner(
        error = error,
        onRetry = actions::refresh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
    )
}

/**
 * Nothing matches: the search's message first, then the filters' (with "Filters", which calls [onOpenFilters]);
 * with neither, "No problems".
 */
@Composable
private fun EmptyProblems(
    options: ProblemsetOptions,
    actions: ProblemsetActions,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        options.query.isNotBlank() ->
            EmptyState(
                title = stringResource(R.string.problemset_empty_search_title, options.query.trim()),
                message = stringResource(R.string.problemset_empty_search_message),
                modifier = modifier.fillMaxSize(),
                action = {
                    PcpSecondaryButton(
                        text = stringResource(R.string.problemset_clear_search),
                        onClick = { actions.search("") },
                    )
                },
            )
        options.filter.activeCount > 0 ->
            EmptyState(
                title = stringResource(R.string.problemset_empty_filtered_title),
                message = stringResource(R.string.problemset_empty_filtered_message),
                modifier = modifier.fillMaxSize(),
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.sm)) {
                        PcpSecondaryButton(
                            text = stringResource(R.string.problemset_clear_filters),
                            onClick = actions::clearFilters,
                        )
                        PcpTextButton(text = stringResource(R.string.problemset_filters), onClick = onOpenFilters)
                    }
                },
            )
        else -> EmptyState(title = stringResource(R.string.problemset_empty_title), modifier = modifier.fillMaxSize())
    }
}

/** The full error state's message; a rejection shows the judge's own words. */
@Composable
private fun errorMessage(error: JudgeError): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.problemset_error_offline)
        is JudgeError.RateLimited -> stringResource(R.string.problemset_error_rate_limited)
        is JudgeError.Unavailable -> stringResource(R.string.problemset_error_unavailable)
        is JudgeError.Malformed -> stringResource(R.string.problemset_error_malformed)
        is JudgeError.Rejected -> error.message.orEmpty()
    }
