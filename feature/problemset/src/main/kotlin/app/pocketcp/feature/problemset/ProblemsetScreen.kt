package app.pocketcp.feature.problemset

import androidx.compose.foundation.background
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
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.ProblemRow
import app.pocketcp.core.ui.RefreshErrorBanner

/** Share of the width the search pane takes next to the list on wide windows (search 1 : list 2). */
private const val SEARCH_PANE_WEIGHT = 1f
private const val LIST_PANE_WEIGHT = 2f

/**
 * Problemset: a title bar with refresh and the spoiler toggles, the search field and sort (above the list on
 * compact width, a side pane on wider windows) and the list area for [state]'s status. [searchText] is the
 * field's text, which leads [state]'s query by the time a search takes. Stateless, so screenshots pass a state.
 */
@Composable
internal fun ProblemsetScreen(
    state: ProblemsetUiState,
    searchText: String,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
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
        if (LocalWindowLayout.current.widthClass == WindowWidthClass.Compact) {
            SearchField(searchText, actions, Modifier.fillMaxWidth().padding(horizontal = spacing.lg))
            SortChips(state.options, actions, Modifier.padding(vertical = spacing.xs))
            ListArea(state, actions, Modifier.weight(1f))
        } else {
            Row(Modifier.weight(1f)) {
                SearchPane(
                    searchText,
                    state.options,
                    actions,
                    Modifier.weight(SEARCH_PANE_WEIGHT).fillMaxHeight(),
                )
                VerticalDivider(color = PocketCpTheme.colors.outline)
                ListArea(state, actions, Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight())
            }
        }
    }
}

/** The status-dependent part: skeleton, full error, empty state or the problems. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListArea(
    state: ProblemsetUiState,
    actions: ProblemsetActions,
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
                EmptyProblems(state.options.query, actions, Modifier.weight(1f))
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
    // A new search or order starts at its first result; recreating the screen (rotation) keeps the position.
    val shownFor = "${options.query}\u0000${options.effectiveSort}"
    var lastShownFor by rememberSaveable { mutableStateOf(shownFor) }
    LaunchedEffect(shownFor) {
        if (shownFor != lastShownFor) {
            lastShownFor = shownFor
            listState.scrollToItem(0)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        items(state.problems, key = { it.id.display }, contentType = { "problem" }) { problem ->
            // Opening a problem is M3.
            ProblemRow(
                problem = problem,
                hideTags = options.hideTags,
                hideRating = options.hideRatings,
                highlighted = false,
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

@Composable
private fun EmptyProblems(
    query: String,
    actions: ProblemsetActions,
    modifier: Modifier = Modifier,
) {
    if (query.isBlank()) {
        EmptyState(title = stringResource(R.string.problemset_empty_title), modifier = modifier.fillMaxSize())
    } else {
        EmptyState(
            title = stringResource(R.string.problemset_empty_search_title, query.trim()),
            message = stringResource(R.string.problemset_empty_search_message),
            modifier = modifier.fillMaxSize(),
            action = {
                PcpSecondaryButton(
                    text = stringResource(R.string.problemset_clear_search),
                    onClick = { actions.search("") },
                )
            },
        )
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
