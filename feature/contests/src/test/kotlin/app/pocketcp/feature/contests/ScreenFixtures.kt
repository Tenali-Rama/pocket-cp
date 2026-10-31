package app.pocketcp.feature.contests

import androidx.compose.runtime.CompositionLocalProvider
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.data.Cached
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.ui.LocalClock

/** The app theme with the fixture clock and the device's window layout, as the app shell provides them. */
internal val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) {
        CompositionLocalProvider(LocalClock provides FixtureClock) { ProvideWindowLayout(content) }
    }
}

internal object NoActions : ContestsActions {
    override fun toggleDivision(division: ContestDivision) = Unit

    override fun toggleKind(kind: ContestKind) = Unit

    override fun clearFilters() = Unit

    override fun refresh() = Unit
}

internal object NoDetailActions : ContestDetailActions {
    override fun open(id: ContestId) = Unit

    override fun close() = Unit

    override fun retryProblems() = Unit
}

/** The fixture list (`contest.list.json`) at its recording time, under [filter]. */
internal fun listState(
    data: List<Contest>? = FixtureContests,
    filter: ContestFilter = ContestFilter(),
    error: JudgeError? = null,
    refreshing: Boolean = false,
) = contestsUiState(
    Cached(data, fetchedAt = data?.let { FixtureNow }, refreshing = refreshing, error = error),
    filter,
    FixtureNow,
    userRefreshing = false,
)

/**
 * The detail of fixture contest [id], opened by the user when [opened] (else the wide-window default), with
 * [problems] as the cached problemset (`null`: not cached yet).
 */
internal fun detailState(
    id: Int,
    opened: Boolean = true,
    problems: List<Problem>? = FixtureProblems,
    problemsRefreshing: Boolean = false,
    problemsError: JudgeError? = null,
    settings: UserSettings = UserSettings(),
) = contestDetailUiState(
    selection = DetailSelection(opened = ContestId(id).takeIf { opened }, shown = ContestId(id)),
    contests = Cached(FixtureContests, FixtureNow, refreshing = false, error = null),
    problemset = Cached(problems, problems?.let { FixtureNow }, problemsRefreshing, problemsError),
    settings = settings,
    now = FixtureNow,
)

/** Fixture contest 2278: the upcoming Div. 3, first in the list, so wide windows show it by default. */
internal const val UPCOMING_ID = 2278

/** Fixture contest 2274: finished, with problem 2274A in `problemset.problems.json`. */
internal const val WITH_PROBLEMS_ID = 2274

/** Fixture contest 2272: finished, with no problems in `problemset.problems.json`. */
internal const val NO_PROBLEMS_ID = 2272
