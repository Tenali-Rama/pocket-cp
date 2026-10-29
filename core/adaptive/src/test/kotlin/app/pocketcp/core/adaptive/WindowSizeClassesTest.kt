package app.pocketcp.core.adaptive

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Material 3 breakpoints, checked on both sides of each boundary. */
class WindowSizeClassesTest {
    private fun widthOf(widthDp: Int) = WindowSizeClass(minWidthDp = widthDp, minHeightDp = 0).widthClass()

    private fun heightOf(heightDp: Int) = WindowSizeClass(minWidthDp = 0, minHeightDp = heightDp).heightClass()

    @Test
    fun widthBreakpoints() {
        assertEquals(WindowWidthClass.Compact, widthOf(599))
        assertEquals(WindowWidthClass.Medium, widthOf(600))
        assertEquals(WindowWidthClass.Medium, widthOf(839))
        assertEquals(WindowWidthClass.Expanded, widthOf(840))
    }

    @Test
    fun heightBreakpoints() {
        assertEquals(WindowHeightClass.Compact, heightOf(479))
        assertEquals(WindowHeightClass.Medium, heightOf(480))
        assertEquals(WindowHeightClass.Medium, heightOf(899))
        assertEquals(WindowHeightClass.Expanded, heightOf(900))
    }
}
