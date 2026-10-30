package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.CodeforcesError
import app.pocketcp.core.codeforces.ContestDto
import app.pocketcp.core.codeforces.ProblemsetDto
import app.pocketcp.core.codeforces.UserDto
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.User
import java.time.Duration
import java.time.Instant

/*
 * Codeforces DTOs and errors to the judge-agnostic domain (ADR 0009). Records the domain can't represent are
 * dropped here, never modelled with placeholder values.
 */

/** A contest; `contest.list` marks gyms only by the request, so the caller passes [isGym]. */
internal fun ContestDto.toContest(isGym: Boolean): Contest =
    Contest(
        id = ContestId(id),
        name = name,
        phase = contestPhase(phase),
        start = startTimeSeconds?.let(Instant::ofEpochSecond),
        duration = Duration.ofSeconds(durationSeconds),
        isGym = isGym,
        kind = contestKind(type),
    )

/** The API's `type` string; a type this app doesn't know yet is [ContestKind.Unknown], never an error. */
internal fun contestKind(type: String): ContestKind =
    when (type) {
        "CF" -> ContestKind.Cf
        "ICPC" -> ContestKind.Icpc
        "IOI" -> ContestKind.Ioi
        else -> ContestKind.Unknown
    }

/** The API's `phase` string; a phase this app doesn't know yet is [ContestPhase.Unknown], never an error. */
internal fun contestPhase(phase: String): ContestPhase =
    when (phase) {
        "BEFORE" -> ContestPhase.Upcoming
        "CODING", "PENDING_SYSTEM_TEST", "SYSTEM_TEST" -> ContestPhase.Running
        "FINISHED" -> ContestPhase.Finished
        else -> ContestPhase.Unknown
    }

/**
 * The problems, each with its `solvedCount` from `problemStatistics` (`null` when it has none). Problems without a
 * `contestId` (e.g. the acmsguru problemset) have no [ProblemId] and are dropped.
 */
internal fun ProblemsetDto.toProblems(): List<Problem> {
    val solved =
        problemStatistics
            .mapNotNull { stat -> stat.contestId?.let { ProblemId(it, stat.index) to stat.solvedCount } }
            .toMap()
    return problems.mapNotNull { dto ->
        val id = dto.contestId?.let { ProblemId(it, dto.index) } ?: return@mapNotNull null
        Problem(id = id, name = dto.name, rating = dto.rating, tags = dto.tags, solvedCount = solved[id])
    }
}

/** A user; unrated users keep `null` ratings, never 0. */
internal fun UserDto.toUser(): User = User(handle = handle, rating = rating, maxRating = maxRating, avatarUrl = avatar)

/** The [JudgeError] a caller sees. Any other HTTP status means a body that isn't the API's, so it is Malformed. */
internal fun CodeforcesError.toJudgeError(): JudgeError =
    when (this) {
        is CodeforcesError.Offline -> JudgeError.Offline(this)
        is CodeforcesError.RateLimited -> JudgeError.RateLimited(this)
        is CodeforcesError.Unavailable -> JudgeError.Unavailable(this)
        is CodeforcesError.Api -> JudgeError.Rejected(comment, this)
        is CodeforcesError.Http, is CodeforcesError.Malformed -> JudgeError.Malformed(this)
    }
