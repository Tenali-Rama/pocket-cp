package app.pocketcp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The clock every countdown and date in the UI reads: the system clock in the device's time zone.
 * Tests provide a fixed one, so screenshots don't depend on when or where they run.
 */
val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }

/**
 * The current instant of [clock], re-read every [tick] while this is composed. Call it once per screen
 * and pass the value to the rows, so a list ticks with one timer instead of one per row.
 */
@Composable
fun rememberNow(
    clock: Clock = LocalClock.current,
    tick: Duration = 1.seconds,
): Instant {
    var now by remember(clock) { mutableStateOf(clock.instant()) }
    LaunchedEffect(clock, tick) {
        while (true) {
            now = clock.instant()
            delay(tick)
        }
    }
    return now
}
