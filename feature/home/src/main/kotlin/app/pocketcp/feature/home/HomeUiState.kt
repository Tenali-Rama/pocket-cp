package app.pocketcp.feature.home

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import java.time.Instant

/** How many upcoming contests Home lists, soonest first (owner may change). */
internal const val UPCOMING_LIMIT = 5

/** Home's contest sections, in display order. */
enum class HomeSection { Running, Upcoming }

/** One non-empty [section] and its contests, in display order. */
@Immutable
data class HomeGroup(
    val section: HomeSection,
    val contests: List<Contest>,
)

/** What the contest area shows (ADR 0011). */
enum class HomeStatus {
    /** No cached data yet: a skeleton. */
    Loading,

    /** No cached data and the refresh failed: a full error with Retry. */
    Error,

    /** Data, but no running or upcoming contest. */
    Empty,

    /** At least one section to show. */
    Loaded,
}

/**
 * The Home screen. [error] is the last refresh's failure: a banner over [groups] when there is data, the full
 * error state otherwise. [userRefreshing] is a refresh the user asked for; a background refresh shows nothing.
 */
@Immutable
data class HomeUiState(
    val status: HomeStatus = HomeStatus.Loading,
    val groups: List<HomeGroup> = emptyList(),
    val error: JudgeError? = null,
    val userRefreshing: Boolean = false,
)

/** The screen state for [cached] contests, sectioned as of [now]. */
internal fun homeUiState(
    cached: Cached<List<Contest>>,
    now: Instant,
    userRefreshing: Boolean,
): HomeUiState {
    val data = cached.data
    if (data == null) {
        val failed = cached.error != null && !cached.refreshing
        return HomeUiState(
            status = if (failed) HomeStatus.Error else HomeStatus.Loading,
            error = cached.error,
            userRefreshing = userRefreshing,
        )
    }
    val groups = homeSections(data, now)
    return HomeUiState(
        status = if (groups.isEmpty()) HomeStatus.Empty else HomeStatus.Loaded,
        groups = groups,
        error = cached.error,
        userRefreshing = userRefreshing,
    )
}

/**
 * Running (ending soonest first) and the next [UPCOMING_LIMIT] upcoming (starting soonest first, unannounced starts
 * last); empty sections are left out. Finished contests and those in an unknown phase are in neither.
 */
internal fun homeSections(
    contests: List<Contest>,
    now: Instant,
): List<HomeGroup> {
    val byPhase = contests.map { it.asOf(now) }.groupBy { it.phase }
    val running = byPhase[ContestPhase.Running].orEmpty().sortedWith(compareBy(nullsLast()) { it.end() })
    val upcoming =
        byPhase[ContestPhase.Upcoming]
            .orEmpty()
            .sortedWith(compareBy(nullsLast()) { it.start })
            .take(UPCOMING_LIMIT)
    return listOf(
        HomeGroup(HomeSection.Running, running),
        HomeGroup(HomeSection.Upcoming, upcoming),
    ).filter { it.contests.isNotEmpty() }
}

/**
 * The next instant after [now] at which a countdown on Home reaches zero (an upcoming contest starting or ending, or
 * a running one ending), or `null` when none will. The cache can be older than these instants (offline, spec §14).
 */
internal fun nextCountdownEnd(
    contests: List<Contest>,
    now: Instant,
): Instant? =
    contests
        .asSequence()
        .flatMap { contest ->
            when (contest.phase) {
                ContestPhase.Upcoming -> listOfNotNull(contest.start, contest.end())
                ContestPhase.Running -> listOfNotNull(contest.end())
                ContestPhase.Finished, ContestPhase.Unknown -> emptyList()
            }
        }.filter { it.isAfter(now) }
        .minOrNull()

/**
 * The contest with the phase it has at [now]: the cached phase, except that an upcoming contest whose start has
 * passed is running, or finished once its length has passed too. A running contest stays running until the judge
 * says otherwise (system testing runs past the end).
 */
internal fun Contest.asOf(now: Instant): Contest {
    val start = start
    return when {
        start == null || phase != ContestPhase.Upcoming || now.isBefore(start) -> this
        now.isBefore(start.plus(duration)) -> copy(phase = ContestPhase.Running)
        else -> copy(phase = ContestPhase.Finished)
    }
}

private fun Contest.end(): Instant? = start?.plus(duration)
