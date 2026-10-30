package app.pocketcp.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class CountdownTest {
    @Test
    fun dayOrMoreShowsDaysThenClock() {
        val remaining =
            Duration
                .ofDays(2)
                .plusHours(3)
                .plusMinutes(4)
                .plusSeconds(5)

        assertEquals("2d 03:04:05", formatCountdown(remaining))
    }

    @Test
    fun belowADayShowsOnlyTheClock() {
        assertEquals("03:04:05", formatCountdown(Duration.ofHours(3).plusMinutes(4).plusSeconds(5)))
    }

    @Test
    fun exactlyOneDayShowsTheDay() {
        assertEquals("1d 00:00:00", formatCountdown(Duration.ofDays(1)))
    }

    @Test
    fun oneSecondBelowADayShowsOnlyTheClock() {
        assertEquals("23:59:59", formatCountdown(Duration.ofDays(1).minusSeconds(1)))
    }

    @Test
    fun negativeIsZero() {
        assertEquals("00:00:00", formatCountdown(Duration.ofSeconds(-5)))
        assertEquals("00:00:00", formatCountdown(Duration.ofDays(-3)))
    }

    @Test
    fun zeroIsZero() {
        assertEquals("00:00:00", formatCountdown(Duration.ZERO))
    }

    @Test
    fun partialSecondsAreDropped() {
        assertEquals("00:00:01", formatCountdown(Duration.ofMillis(1_999)))
    }

    @Test
    fun lengthShowsHoursAndMinutes() {
        assertEquals("02:15", formatLength(Duration.ofMinutes(135)))
        assertEquals("3d 02:15", formatLength(Duration.ofDays(3).plusMinutes(135)))
    }
}
