package app.pocketcp.feature.contests

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import java.time.Instant

/** The three parts of the list, in display order. */
enum class ContestSection { Running, Upcoming, Past }

/** One non-empty [section] and its contests, in display order. */
@Immutable
data class ContestGroup(
    val section: ContestSection,
    val contests: List<Contest>,
)

/** The selected chips. An empty set means that dimension is not filtered. */
@Immutable
data class ContestFilter(
    val divisions: Set<ContestDivision> = emptySet(),
    val kinds: Set<ContestKind> = emptySet(),
) {
    val isEmpty: Boolean get() = divisions.isEmpty() && kinds.isEmpty()

    fun matches(contest: Contest): Boolean =
        (divisions.isEmpty() || contest.division in divisions) && (kinds.isEmpty() || contest.kind in kinds)

    companion object {
        /** The kinds offered as chips: every [ContestKind] the judge names. */
        val KindChoices: List<ContestKind> = ContestKind.entries - ContestKind.Unknown
    }
}

/** What the list area shows (ADR 0011). */
enum class ContestsStatus {
    /** No cached data yet: a skeleton. */
    Loading,

    /** No cached data and the refresh failed: a full error with Retry. */
    Error,

    /** Data, but nothing passes the filter (or the judge lists no contests). */
    Empty,

    /** At least one section to show. */
    Loaded,
}

/**
 * The Contests screen. [error] is the last refresh's failure: a banner over [groups] when there is data, the
 * full error state otherwise. [userRefreshing] is a refresh the user asked for (pull or button); a background
 * refresh never shows an indicator.
 */
@Immutable
data class ContestsUiState(
    val status: ContestsStatus = ContestsStatus.Loading,
    val groups: List<ContestGroup> = emptyList(),
    val filter: ContestFilter = ContestFilter(),
    val error: JudgeError? = null,
    val userRefreshing: Boolean = false,
)

/** The screen state for [cached] contests under [filter], sectioned as of [now]. */
internal fun contestsUiState(
    cached: Cached<List<Contest>>,
    filter: ContestFilter,
    now: Instant,
    userRefreshing: Boolean,
): ContestsUiState {
    val data = cached.data
    if (data == null) {
        val failed = cached.error != null && !cached.refreshing
        return ContestsUiState(
            status = if (failed) ContestsStatus.Error else ContestsStatus.Loading,
            filter = filter,
            error = cached.error,
            userRefreshing = userRefreshing,
        )
    }
    val groups = sections(data.filter(filter::matches), now)
    return ContestsUiState(
        status = if (groups.isEmpty()) ContestsStatus.Empty else ContestsStatus.Loaded,
        groups = groups,
        filter = filter,
        error = cached.error,
        userRefreshing = userRefreshing,
    )
}

/**
 * Running (ending soonest first), Upcoming (starting soonest first) and Past (newest first); empty sections are
 * left out. Contests without a start go last in their section; a contest in an unknown phase is in none.
 */
internal fun sections(
    contests: List<Contest>,
    now: Instant,
): List<ContestGroup> {
    val bySection = contests.map { it.asOf(now) }.groupBy { it.section() }
    val running = bySection[ContestSection.Running].orEmpty().sortedWith(compareBy(nullsLast()) { it.end() })
    val upcoming = bySection[ContestSection.Upcoming].orEmpty().sortedWith(compareBy(nullsLast()) { it.start })
    val past =
        bySection[ContestSection.Past].orEmpty().sortedWith(compareBy(nullsLast(reverseOrder())) { it.start })
    return listOf(
        ContestGroup(ContestSection.Running, running),
        ContestGroup(ContestSection.Upcoming, upcoming),
        ContestGroup(ContestSection.Past, past),
    ).filter { it.contests.isNotEmpty() }
}

/**
 * The next instant at which [asOf] moves one of [contests] to another section (an upcoming contest starting or
 * ending), or `null` when none will. The cache can be older than the contest's start (offline, spec §14).
 */
internal fun nextPhaseChange(
    contests: List<Contest>,
    now: Instant,
): Instant? =
    contests
        .asSequence()
        .filter { it.phase == ContestPhase.Upcoming }
        .flatMap { listOfNotNull(it.start, it.end()) }
        .filter { it.isAfter(now) }
        .minOrNull()

/**
 * The contest with the phase it has at [now]: the cached phase, except that an upcoming contest whose start has
 * passed is running, or finished once its length has passed too. A running contest stays running until the judge
 * says otherwise (system testing runs past the end).
 */
internal fun Contest.asOf(now: Instant): Contest {
    val start = start ?: return this
    val end = end() ?: return this
    return when {
        phase != ContestPhase.Upcoming || now.isBefore(start) -> this
        now.isBefore(end) -> copy(phase = ContestPhase.Running)
        else -> copy(phase = ContestPhase.Finished)
    }
}

private fun Contest.end(): Instant? = start?.plus(duration)

private fun Contest.section(): ContestSection? =
    when (phase) {
        ContestPhase.Running -> ContestSection.Running
        ContestPhase.Upcoming -> ContestSection.Upcoming
        ContestPhase.Finished -> ContestSection.Past
        ContestPhase.Unknown -> null
    }
