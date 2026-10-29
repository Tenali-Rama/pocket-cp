package app.pocketcp.core.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTypeTest {
    @Test
    fun compactWidth_usesBottomBar() {
        assertEquals(NavigationType.BottomBar, navigationTypeOf(WindowWidthClass.Compact))
    }

    @Test
    fun mediumWidth_usesRail() {
        assertEquals(NavigationType.Rail, navigationTypeOf(WindowWidthClass.Medium))
    }

    @Test
    fun expandedWidth_usesRail() {
        assertEquals(NavigationType.Rail, navigationTypeOf(WindowWidthClass.Expanded))
    }
}
