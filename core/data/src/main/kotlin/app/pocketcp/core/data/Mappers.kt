package app.pocketcp.core.data

import app.pocketcp.core.database.ContestEntity
import app.pocketcp.core.database.ProblemEntity
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import java.time.Duration
import java.time.Instant

/** Stored to the second, as the judge publishes it. */
internal fun Contest.toEntity(): ContestEntity =
    ContestEntity(
        id = id.value,
        name = name,
        phase = phase.name,
        startEpochSeconds = start?.epochSecond,
        durationSeconds = duration.seconds,
        isGym = isGym,
        kind = kind.name,
    )

/** A phase or kind name this version doesn't know (e.g. written by a newer one) reads as `Unknown`. */
internal fun ContestEntity.toDomain(): Contest =
    Contest(
        id = ContestId(id),
        name = name,
        phase = ContestPhase.entries.firstOrNull { it.name == phase } ?: ContestPhase.Unknown,
        start = startEpochSeconds?.let(Instant::ofEpochSecond),
        duration = Duration.ofSeconds(durationSeconds),
        isGym = isGym,
        kind = ContestKind.entries.firstOrNull { it.name == kind } ?: ContestKind.Unknown,
    )

internal fun Problem.toEntity(): ProblemEntity =
    ProblemEntity(
        contestId = id.contestId,
        index = id.index,
        name = name,
        rating = rating,
        tags = tags,
        solvedCount = solvedCount,
    )

internal fun ProblemEntity.toDomain(): Problem =
    Problem(
        id = ProblemId(contestId, index),
        name = name,
        rating = rating,
        tags = tags,
        solvedCount = solvedCount,
    )
