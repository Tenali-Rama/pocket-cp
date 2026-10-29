package app.pocketcp.core.adaptive

import androidx.window.core.layout.WindowSizeClass

/** Material 3 window width class: compact < 600dp ≤ medium < 840dp ≤ expanded. */
enum class WindowWidthClass { Compact, Medium, Expanded }

/** Material 3 window height class: compact < 480dp ≤ medium < 900dp ≤ expanded. */
enum class WindowHeightClass { Compact, Medium, Expanded }

internal fun WindowSizeClass.widthClass(): WindowWidthClass =
    when {
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowWidthClass.Expanded
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowWidthClass.Medium
        else -> WindowWidthClass.Compact
    }

internal fun WindowSizeClass.heightClass(): WindowHeightClass =
    when {
        isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> WindowHeightClass.Expanded
        isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowHeightClass.Medium
        else -> WindowHeightClass.Compact
    }
