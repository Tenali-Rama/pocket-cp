package app.pocketcp.feature.contests

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import java.time.Duration
import java.time.Instant

/** What the header says about when the contest runs; the UI turns it into a ticking countdown or a date. */
sealed interface ContestTiming {
    /** Upcoming: "Starts in" a countdown to [start]. */
    data class StartsIn(
        val start: Instant,
    ) : ContestTiming

    /** Running: "Ends in" a countdown to [end]. */
    data class EndsIn(
        val end: Instant,
    ) : ContestTiming

    /** Finished: its [start] date and [length]. */
    data class Held(
        val start: Instant,
        val length: Duration,
    ) : ContestTiming

    /** The judge has published no start time. */
    data object NotAnnounced : ContestTiming

    /** An unknown phase: nothing to say. */
    data object None : ContestTiming
}

/** What the detail shows for the contest itself. */
enum class DetailStatus {
    /** No contest is shown (nothing selected, or no default yet). */
    None,

    /** The contest list isn't cached yet. */
    Loading,

    /** The cached list has no contest with the shown id. */
    NotFound,

    /** [ContestDetailUiState.contest] is set. */
    Loaded,
}

/** The problems part of the detail (ADR 0011 for the problemset cache). */
enum class ProblemsStatus {
    /** No cached problemset yet: a skeleton. */
    Loading,

    /** No cached problemset and its refresh failed: a full error with Retry. */
    Error,

    /** The contest hasn't finished and the problemset has none of its problems yet. */
    NotYetAdded,

    /** A finished contest without problems in the problemset. */
    Empty,

    /** [ContestDetailUiState.problems] is non-empty. */
    Loaded,
}

/**
 * The contest detail. [selected] is the contest the user opened (compact width shows its page); [shown] is the one
 * displayed, which on wide windows falls back to the list's first contest. [contest] is as of the state's now
 * (an upcoming contest whose start passed is running). [problemsError] is the problemset's last refresh failure.
 */
@Immutable
data class ContestDetailUiState(
    val selected: ContestId? = null,
    val shown: ContestId? = null,
    val status: DetailStatus = DetailStatus.None,
    val contest: Contest? = null,
    val timing: ContestTiming = ContestTiming.None,
    val problemsStatus: ProblemsStatus = ProblemsStatus.Loading,
    val problems: List<Problem> = emptyList(),
    val problemsError: JudgeError? = null,
    val hideTags: Boolean = false,
    val hideRatings: Boolean = false,
)

/** The contest the user [opened] (or `null`), and the one [shown]: [opened], or else the wide-window default. */
internal data class DetailSelection(
    val opened: ContestId?,
    val shown: ContestId?,
)

/**
 * The detail of [selection]'s shown contest in [contests] and [problemset], as of [now], with the spoiler flags of
 * [settings].
 */
internal fun contestDetailUiState(
    selection: DetailSelection,
    contests: Cached<List<Contest>>,
    problemset: Cached<List<Problem>>,
    settings: UserSettings,
    now: Instant,
): ContestDetailUiState {
    val base =
        ContestDetailUiState(
            selected = selection.opened,
            shown = selection.shown,
            hideTags = settings.hideProblemTags,
            hideRatings = settings.hideProblemRatings,
        )
    val shown = selection.shown
    val data = contests.data
    val contest = shown?.let { id -> data?.find { it.id == id } }?.asOf(now)
    return when {
        shown == null -> base
        data == null -> base.copy(status = DetailStatus.Loading)
        contest == null -> base.copy(status = DetailStatus.NotFound)
        else -> {
            val problems = problemset.data?.let { problemsOf(shown, it) }
            base.copy(
                status = DetailStatus.Loaded,
                contest = contest,
                timing = contest.timing(),
                problemsStatus = problemsStatus(contest, problems, problemset),
                problems = problems.orEmpty(),
                problemsError = problemset.error,
            )
        }
    }
}

/** [contest]'s problems in [problemset], ordered by index (`A`, `B1`, `B2`, `C`). */
internal fun problemsOf(
    contest: ContestId,
    problemset: List<Problem>,
): List<Problem> = problemset.filter { it.id.contestId == contest.value }.sortedBy { it.id.index }

/** The header's timing for this contest in its (already time-adjusted) phase. */
internal fun Contest.timing(): ContestTiming {
    val start = start
    return when {
        phase == ContestPhase.Unknown -> ContestTiming.None
        start == null -> ContestTiming.NotAnnounced
        phase == ContestPhase.Upcoming -> ContestTiming.StartsIn(start)
        phase == ContestPhase.Running -> ContestTiming.EndsIn(start.plus(duration))
        else -> ContestTiming.Held(start, duration)
    }
}

private fun problemsStatus(
    contest: Contest,
    problems: List<Problem>?,
    problemset: Cached<List<Problem>>,
): ProblemsStatus =
    when {
        problems == null ->
            if (problemset.error != null && !problemset.refreshing) ProblemsStatus.Error else ProblemsStatus.Loading
        problems.isNotEmpty() -> ProblemsStatus.Loaded
        contest.phase == ContestPhase.Finished -> ProblemsStatus.Empty
        else -> ProblemsStatus.NotYetAdded
    }
