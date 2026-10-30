package app.pocketcp.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private val Epoch: Instant = Instant.parse("2026-10-31T12:00:00Z")

@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class NowTest {
    @get:Rule
    val rule = createComposeRule()

    /** A clock that reads the compose rule's main clock, so time only moves when the test advances it. */
    private val testClock =
        object : Clock() {
            override fun instant(): Instant = Epoch.plusMillis(rule.mainClock.currentTime)

            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = this
        }

    @Test
    fun valueAdvancesWithTheMainClockOncePerTick() {
        rule.mainClock.autoAdvance = false
        var now = Instant.MIN
        rule.setContent {
            CompositionLocalProvider(LocalClock provides testClock) { now = rememberNow() }
        }
        rule.waitForIdle()
        val first = now
        assertEquals(testClock.instant(), first)

        rule.mainClock.advanceTimeBy(HALF_TICK_MS, ignoreFrameDuration = true)
        rule.waitForIdle()
        assertEquals("no tick before a second has passed", first, now)

        rule.mainClock.advanceTimeBy(MORE_THAN_HALF_TICK_MS, ignoreFrameDuration = true)
        rule.waitForIdle()
        val second = now
        assertTrue("ticked after a second: $first -> $second", second > first)
        assertTrue("never ahead of the clock", second <= testClock.instant())

        rule.mainClock.advanceTimeBy(TICK_MS, ignoreFrameDuration = true)
        rule.waitForIdle()
        assertTrue("ticked again: $second -> $now", now > second)
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val HALF_TICK_MS = 500L
        const val MORE_THAN_HALF_TICK_MS = 600L
    }
}
