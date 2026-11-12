package app.pocketcp.core.problemview

import androidx.compose.runtime.Immutable
import app.pocketcp.core.data.Cached
import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage

/** What the problem view shows (ADR 0011: cache first, a failed refresh over data is a banner). */
enum class ProblemStatus {
    /** No cached statement yet and a refresh is running: a skeleton. */
    Loading,

    /** [ProblemUiState.statement] is set; a non-null [ProblemUiState.error] shows as a banner over it. */
    Loaded,

    /** No cached statement and the refresh failed (offline, rate limited, unavailable or rejected): Retry. */
    Error,

    /** No cached statement and the page could not be parsed (`JudgeError.Malformed`). */
    Unparsable,
}

/**
 * The problem view of [problem]: its [statement] in [language] when cached, the last refresh's [error], and the
 * reading settings [textSize] and [width].
 */
@Immutable
data class ProblemUiState(
    val problem: ProblemId,
    val status: ProblemStatus = ProblemStatus.Loading,
    val statement: Statement? = null,
    val error: JudgeError? = null,
    val language: StatementLanguage = StatementLanguage.En,
    val textSize: TextSizePreference = TextSizePreference.Default,
    val width: StatementWidthPreference = StatementWidthPreference.Comfortable,
)

/** The state of [problem] for its cached statement in [language] and the reading settings in [settings]. */
internal fun problemUiState(
    problem: ProblemId,
    language: StatementLanguage,
    cached: Cached<Statement>,
    settings: UserSettings,
): ProblemUiState {
    val error = cached.error
    val status =
        when {
            cached.data != null -> ProblemStatus.Loaded
            cached.refreshing || error == null -> ProblemStatus.Loading
            error is JudgeError.Malformed -> ProblemStatus.Unparsable
            else -> ProblemStatus.Error
        }
    return ProblemUiState(
        problem = problem,
        status = status,
        statement = cached.data,
        error = error,
        language = language,
        textSize = settings.statementTextSize,
        width = settings.statementWidth,
    )
}
