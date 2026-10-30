package app.pocketcp.core.model

/** Stable id of a judge, e.g. `codeforces`. Cached data is keyed by it, so never rename an existing one. */
@JvmInline
value class JudgeId(
    val value: String,
)
