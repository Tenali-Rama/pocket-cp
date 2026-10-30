package app.pocketcp.core.model

/** A problem's contest and its index within it, e.g. `4` and `A`; shown as [display] (`4A`). */
data class ProblemId(
    val contestId: Int,
    val index: String,
) {
    val display: String get() = "$contestId$index"
}

/** A problemset problem. [rating] and [solvedCount] are `null` when the judge has none. */
data class Problem(
    val id: ProblemId,
    val name: String,
    val rating: Int?,
    val tags: List<String>,
    val solvedCount: Int?,
)
