package app.pocketcp.core.ui

import java.time.Duration
import java.util.Locale

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L

/**
 * A countdown as `2d 03:04:05` when [remaining] is a day or more, `03:04:05` below that,
 * and `00:00:00` once it is negative. Partial seconds are dropped.
 */
fun formatCountdown(remaining: Duration): String {
    val total = if (remaining.isNegative) 0L else remaining.seconds
    val days = total / SECONDS_PER_DAY
    val clock =
        String.format(
            Locale.ROOT,
            "%02d:%02d:%02d",
            total % SECONDS_PER_DAY / SECONDS_PER_HOUR,
            total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
            total % SECONDS_PER_MINUTE,
        )
    return if (days > 0) "${days}d $clock" else clock
}

/** A contest's length as `02:15` (hours and minutes), or `3d 02:15` when it is a day or more. */
internal fun formatLength(length: Duration): String {
    val total = if (length.isNegative) 0L else length.seconds
    val days = total / SECONDS_PER_DAY
    val clock =
        String.format(
            Locale.ROOT,
            "%02d:%02d",
            total % SECONDS_PER_DAY / SECONDS_PER_HOUR,
            total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
        )
    return if (days > 0) "${days}d $clock" else clock
}
