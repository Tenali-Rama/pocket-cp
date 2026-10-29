package app.pocketcp.core.screenshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class CaptureMatrixTest {
    @Test
    fun fileName_isNameDeviceTheme() {
        assertEquals("contests_tablet_dark.png", ScreenshotConfig(Device.Tablet, dark = true).fileName("contests"))
        assertEquals("contests_foldable_light.png", ScreenshotConfig(Device.Foldable, dark = false).fileName("contests"))
        assertEquals("contests_font150_phone_dark.png", ScreenshotConfig(fontScale = 1.5f).fileName("contests_font150"))
    }

    @Test
    fun matrix_isFourDevicesAtTheAgreedSizes() {
        val sizes = Device.entries.associate { it.id to (it.widthDp to it.heightDp) }

        assertEquals(
            mapOf(
                "phone" to (411 to 914),
                "tablet" to (1280 to 800),
                "foldable" to (673 to 841),
                "desktop" to (1440 to 900),
            ),
            sizes,
        )
    }

    @Test
    fun runAllCaptures_withoutFailures_runsEachOnce() {
        val ran = mutableListOf<Int>()

        runAllCaptures(listOf({ ran += 1 }, { ran += 2 }))

        assertEquals(listOf(1, 2), ran)
    }

    @Test
    fun runAllCaptures_afterAFailure_stillRunsTheRestThenThrowsTheFirst() {
        val first = AssertionError("golden missing")
        val second = AssertionError("pixels changed")
        val ran = mutableListOf<Int>()

        try {
            runAllCaptures(
                listOf(
                    {
                        ran += 1
                        throw first
                    },
                    { ran += 2 },
                    {
                        ran += 3
                        throw second
                    },
                ),
            )
            fail("expected the first capture failure")
        } catch (e: AssertionError) {
            assertSame(first, e)
            assertEquals(listOf(second), e.suppressed.toList())
        }
        assertEquals(listOf(1, 2, 3), ran)
    }
}
