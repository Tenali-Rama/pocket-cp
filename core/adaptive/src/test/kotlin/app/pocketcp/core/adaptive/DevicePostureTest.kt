package app.pocketcp.core.adaptive

import android.graphics.Rect
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.window.testing.layout.FoldingFeature as testFoldingFeature

/** Robolectric only because fold bounds are an `android.graphics.Rect`. */
@RunWith(AndroidJUnit4::class)
class DevicePostureTest {
    private val window = Rect(0, 0, 1000, 800)

    private fun fold(
        state: FoldingFeature.State,
        orientation: FoldingFeature.Orientation,
    ) = testFoldingFeature(
        windowBounds = window,
        center = if (orientation == FoldingFeature.Orientation.VERTICAL) 500 else 400,
        size = 20,
        state = state,
        orientation = orientation,
    )

    @Test
    fun halfOpenedVerticalFold_isBookWithHinge() {
        val posture = devicePostureOf(listOf(fold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.VERTICAL)))

        assertEquals(DevicePosture.Book(IntRect(490, 0, 510, 800)), posture)
    }

    @Test
    fun halfOpenedHorizontalFold_isTabletopWithHinge() {
        val posture = devicePostureOf(listOf(fold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.HORIZONTAL)))

        assertEquals(DevicePosture.Tabletop(IntRect(0, 390, 1000, 410)), posture)
    }

    @Test
    fun flatFold_isFlat() {
        val vertical = devicePostureOf(listOf(fold(FoldingFeature.State.FLAT, FoldingFeature.Orientation.VERTICAL)))
        val horizontal = devicePostureOf(listOf(fold(FoldingFeature.State.FLAT, FoldingFeature.Orientation.HORIZONTAL)))

        assertEquals(DevicePosture.Flat, vertical)
        assertEquals(DevicePosture.Flat, horizontal)
    }

    @Test
    fun noDisplayFeatures_isFlat() {
        assertEquals(DevicePosture.Flat, devicePostureOf(emptyList()))
    }

    @Test
    fun flatFeatureBeforeHalfOpenedOne_usesTheHalfOpenedFold() {
        val features =
            listOf(
                fold(FoldingFeature.State.FLAT, FoldingFeature.Orientation.HORIZONTAL),
                fold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.VERTICAL),
            )

        assertEquals(DevicePosture.Book(IntRect(490, 0, 510, 800)), devicePostureOf(features))
    }
}
