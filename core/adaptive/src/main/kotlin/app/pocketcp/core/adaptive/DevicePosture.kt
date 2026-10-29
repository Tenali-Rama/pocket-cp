package app.pocketcp.core.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntRect
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature

/** How a foldable is held. [Flat] covers phones, tablets, desktops and fully opened or closed foldables. */
@Immutable
sealed interface DevicePosture {
    /** No half-opened fold: lay out as one flat screen. */
    data object Flat : DevicePosture

    /** Half opened around a vertical hinge, like a book. [hinge] is in window pixels. */
    data class Book(
        val hinge: IntRect,
    ) : DevicePosture

    /** Half opened around a horizontal hinge, like a laptop. [hinge] is in window pixels. */
    data class Tabletop(
        val hinge: IntRect,
    ) : DevicePosture
}

/** The posture for a window's display features; only a half-opened [FoldingFeature] leaves [DevicePosture.Flat]. */
fun devicePostureOf(displayFeatures: List<DisplayFeature>): DevicePosture {
    val fold =
        displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull { it.state == FoldingFeature.State.HALF_OPENED }
            ?: return DevicePosture.Flat
    val hinge = fold.bounds.let { IntRect(it.left, it.top, it.right, it.bottom) }
    return if (fold.orientation == FoldingFeature.Orientation.VERTICAL) {
        DevicePosture.Book(hinge)
    } else {
        DevicePosture.Tabletop(hinge)
    }
}
