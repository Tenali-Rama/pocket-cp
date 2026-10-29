package app.pocketcp.core.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

/** Everything a screen needs to pick its layout. Read it from [LocalWindowLayout]. */
@Immutable
data class WindowLayout(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val posture: DevicePosture,
    val navigationType: NavigationType,
)

/** The current [WindowLayout], provided once at the app root by [ProvideWindowLayout]. Reading it unprovided fails. */
val LocalWindowLayout =
    compositionLocalOf<WindowLayout> {
        error("No WindowLayout provided: wrap the content in ProvideWindowLayout")
    }

/**
 * The live [WindowLayout] of the window hosting this composition: size classes from
 * [currentWindowAdaptiveInfo], posture from Jetpack WindowManager's [WindowInfoTracker].
 * Updates on resize, rotation and fold changes.
 */
@Composable
fun rememberWindowLayout(): WindowLayout {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val context = LocalContext.current
    val postureFlow =
        remember(context) {
            WindowInfoTracker
                .getOrCreate(context)
                .windowLayoutInfo(context)
                .map { devicePostureOf(it.displayFeatures) }
        }
    val posture by postureFlow.collectAsState(initial = DevicePosture.Flat)
    val widthClass = sizeClass.widthClass()
    val heightClass = sizeClass.heightClass()
    return remember(widthClass, heightClass, posture) {
        WindowLayout(widthClass, heightClass, posture, navigationTypeOf(widthClass))
    }
}

/** Provides [LocalWindowLayout] from [rememberWindowLayout] to [content]. Call it once, at the app root. */
@Composable
fun ProvideWindowLayout(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWindowLayout provides rememberWindowLayout(), content = content)
}
