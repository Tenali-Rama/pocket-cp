package app.pocketcp.core.model

/**
 * The only way the data layer reaches a judge (ADR 0009).
 * Every method throws only [JudgeError]; implementations map their own failures before they leave.
 */
interface JudgeSource {
    val judge: JudgeId

    /** All contests, or all gyms when [gym] is true. */
    suspend fun contests(gym: Boolean): List<Contest>

    /** The whole problemset. */
    suspend fun problemset(): List<Problem>

    /** The users with these [handles]. */
    suspend fun users(handles: List<String>): List<User>

    /** The [problem]'s native statement in [language]. */
    suspend fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Statement

    /** The page URL for [problem]'s statement in [language], for the WebView fallback. */
    fun problemUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String
}
