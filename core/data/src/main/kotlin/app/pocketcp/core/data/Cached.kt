package app.pocketcp.core.data

import app.pocketcp.core.model.JudgeError
import java.time.Instant

/**
 * A cached dataset as the UI sees it (ADR 0011).
 * - [data] is `null` only when the dataset was never fetched; after a fetch it is the cache, even if empty.
 * - [fetchedAt] is when the cache was last written, or `null` if never.
 * - [refreshing] is true while a background refresh runs.
 * - [error] is the last refresh's failure, cleared by the next successful refresh. It never clears [data].
 */
data class Cached<out T>(
    val data: T?,
    val fetchedAt: Instant?,
    val refreshing: Boolean,
    val error: JudgeError?,
)
