package app.pocketcp.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.EmptyState
import app.pocketcp.core.designsystem.component.ErrorState
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpCard
import app.pocketcp.core.designsystem.component.PcpIconButton
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.ui.ContestRow
import app.pocketcp.core.ui.RefreshErrorBanner
import java.time.Instant

/** Maestro id of the guest card. Never rename it. */
internal const val GUEST_TAG = "home_guest"

/**
 * Home: a title bar with a refresh button, the guest card and the contest area for [state]'s status. On an expanded
 * window the guest card and Running sit beside Upcoming. [now] (from `rememberNow()`) drives the rows' countdowns.
 * Stateless, so screenshots pass a fixed state and time.
 */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    now: Instant,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        PcpTopAppBar(
            title = stringResource(R.string.home_label),
            actions = {
                PcpIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.home_refresh),
                    onClick = actions::refresh,
                )
            },
        )
        when (state.status) {
            HomeStatus.Loading -> {
                GuestCard()
                LoadingPlaceholder(Modifier.fillMaxWidth())
            }
            HomeStatus.Error -> {
                GuestCard()
                ErrorState(
                    message = errorMessage(state.error ?: JudgeError.Unavailable()),
                    onRetry = actions::refresh,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            HomeStatus.Empty -> {
                state.error?.let { Banner(it, actions) }
                GuestCard()
                EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    message = stringResource(R.string.home_empty_message),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            HomeStatus.Loaded -> {
                state.error?.let { Banner(it, actions) }
                Sections(state, now, actions, Modifier.weight(1f))
            }
        }
    }
}

/** The guest card and the sections: one list, or two panes side by side on an expanded window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sections(
    state: HomeUiState,
    now: Instant,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = state.userRefreshing, onRefresh = actions::refresh, modifier = modifier) {
        if (LocalWindowLayout.current.widthClass == WindowWidthClass.Expanded) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight()) {
                    item(key = GUEST_TAG, contentType = GUEST_TAG) { GuestCard() }
                    state.groups.dropLast(1).forEach { section(it, now) }
                }
                VerticalDivider(color = PocketCpTheme.colors.outline)
                LazyColumn(Modifier.weight(1f).fillMaxHeight()) {
                    // Loaded always has a group; the last one gets the right pane, so it is never empty.
                    state.groups.lastOrNull()?.let { section(it, now) }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = GUEST_TAG, contentType = GUEST_TAG) { GuestCard() }
                state.groups.forEach { section(it, now) }
            }
        }
    }
}

private fun LazyListScope.section(
    group: HomeGroup,
    now: Instant,
) {
    item(key = "section_${group.section.name}", contentType = "section") { SectionHeader(group.section.title) }
    items(group.contests, key = { it.id.value }, contentType = { "contest" }) { contest ->
        // Opening a contest from Home needs cross-feature navigation (deep links, M4).
        ContestRow(contest = contest, now = now, onClick = {})
    }
}

/** Until sign-in lands (M4): what Home will show once signed in. No action yet. */
@Composable
private fun GuestCard() {
    val spacing = PocketCpTheme.spacing
    PcpCard(
        Modifier
            .testTag(GUEST_TAG)
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.home_guest),
            style = PocketCpTheme.typography.body,
            color = PocketCpTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.cardVertical),
        )
    }
}

@Composable
private fun Banner(
    error: JudgeError,
    actions: HomeActions,
) {
    val spacing = PocketCpTheme.spacing
    RefreshErrorBanner(
        error = error,
        onRetry = actions::refresh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
    )
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
private val HomeSection.title: Int
    get() =
        when (this) {
            HomeSection.Running -> R.string.home_section_running
            HomeSection.Upcoming -> R.string.home_section_upcoming
        }

/** The full error state's message; a rejection shows the judge's own words. */
@Composable
private fun errorMessage(error: JudgeError): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.home_error_offline)
        is JudgeError.RateLimited -> stringResource(R.string.home_error_rate_limited)
        is JudgeError.Unavailable -> stringResource(R.string.home_error_unavailable)
        is JudgeError.Malformed -> stringResource(R.string.home_error_malformed)
        is JudgeError.Rejected -> error.message.orEmpty()
    }
