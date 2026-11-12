package app.pocketcp.core.data

import app.pocketcp.core.database.ContestEntity
import app.pocketcp.core.database.ProblemEntity
import app.pocketcp.core.database.StatementEntity
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant

/**
 * The version of the JSON in `StatementEntity.document` (ADR 0017). Bump it when stored statements must be
 * refetched (e.g. the parser got better); rows of another version read as missing.
 */
internal const val STATEMENT_FORMAT_VERSION = 1

private val statementJson = Json

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

/** Keyed by the statement's own problem and language, stamped with the current format version. */
internal fun Statement.toEntity(fetchedAtEpochMillis: Long): StatementEntity =
    StatementEntity(
        contestId = problem.contestId,
        index = problem.index,
        language = language.key,
        document = statementJson.encodeToString(Statement.serializer(), this),
        formatVersion = STATEMENT_FORMAT_VERSION,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

/** The stored statement, or null when it was written in another format version or can't be decoded. */
@Suppress("SwallowedException")
internal fun StatementEntity.toDomain(): Statement? {
    if (formatVersion != STATEMENT_FORMAT_VERSION) return null
    return try {
        statementJson.decodeFromString(Statement.serializer(), document)
    } catch (corrupt: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException: a corrupt row reads as missing and is refetched.
        null
    }
}
