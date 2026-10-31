package app.pocketcp.feature.problemset

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
import app.pocketcp.core.ui.ProblemRow
import app.pocketcp.core.ui.RefreshErrorBanner

/** Shares of the width on wide windows: search 1 : list 2, plus filters 1 on expanded width. */
private const val SEARCH_PANE_WEIGHT = 1f
private const val LIST_PANE_WEIGHT = 2f
private const val FILTER_PANE_WEIGHT = 1f

/**
 * Problemset: a title bar with refresh and the spoiler toggles, the search field, "Filters", "Random" and sort
 * (above the list on compact width, a side pane on wider windows), the list area for [state]'s status, and the
 * filters: a sheet over the screen on compact and medium width ([filtersOpen] at first), a panel right of the list
 * on expanded width. [searchText] is the field's text, which leads [state]'s query by the time a search takes.
 * Stateless, so screenshots pass a state.
 */
@Composable
internal fun ProblemsetScreen(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
    filtersOpen: Boolean = false,
) {
    val spacing = PocketCpTheme.spacing
    val widthClass = LocalWindowLayout.current.widthClass
    var sheetOpen by rememberSaveable { mutableStateOf(filtersOpen) }
    val openFilters = { sheetOpen = true }
    Box(modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
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
            if (widthClass == WindowWidthClass.Compact) {
                SearchField(searchText, actions, Modifier.fillMaxWidth().padding(horizontal = spacing.lg))
                FilterAndRandomButtons(
                    state,
                    actions,
                    openFilters,
                    Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm),
                )
                SortChips(state.options, actions, Modifier.padding(vertical = spacing.xs))
                ListArea(state, actions, openFilters, Modifier.weight(1f))
            } else {
                WidePanes(state, searchText, actions, openFilters, Modifier.weight(1f))
            }
        }
        if (sheetOpen && widthClass != WindowWidthClass.Expanded) {
            FilterSheet(state, actions, onDismiss = { sheetOpen = false })
        }
    }
}

/** Medium: search pane | list. Expanded: search pane | list | filter panel. */
@Composable
private fun WidePanes(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LocalWindowLayout.current.widthClass == WindowWidthClass.Expanded
    Row(modifier) {
        SearchPane(searchText, state.options, actions, Modifier.weight(SEARCH_PANE_WEIGHT).fillMaxHeight()) {
            if (expanded) RandomButton(state, actions) else FilterAndRandomButtons(state, actions, onOpenFilters)
        }
        VerticalDivider(color = PocketCpTheme.colors.outline)
        // Expanded width has no sheet to open: the panel is already beside the list.
        ListArea(
            state,
            actions,
            onOpenFilters.takeUnless { expanded },
            Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight(),
        )
        if (expanded) {
            VerticalDivider(color = PocketCpTheme.colors.outline)
            FilterPanel(state, actions, Modifier.weight(FILTER_PANE_WEIGHT).fillMaxHeight())
        }
    }
}

/** The status-dependent part: skeleton, full error, empty state or the problems. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListArea(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
    onOpenFilters: (() -> Unit)?,
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
                    ProblemList(state)
                }
            }
    }
}

@Composable
private fun ProblemList(state: ProblemsetUiState) {
    val listState = rememberLazyListState()
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
            // Opening a problem is M3.
            ProblemRow(
                problem = problem,
                hideTags = options.hideTags,
                hideRating = options.hideRatings,
                highlighted = problem.id.display == pick.display,
                onClick = {},
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
 * Nothing matches: the search's message first, then the filters' (with "Filters" when [onOpenFilters] opens the
 * sheet); with neither, "No problems".
 */
@Composable
private fun EmptyProblems(
    options: ProblemsetOptions,
    actions: ProblemsetActions,
    onOpenFilters: (() -> Unit)?,
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
                        if (onOpenFilters != null) {
                            PcpTextButton(text = stringResource(R.string.problemset_filters), onClick = onOpenFilters)
                        }
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
