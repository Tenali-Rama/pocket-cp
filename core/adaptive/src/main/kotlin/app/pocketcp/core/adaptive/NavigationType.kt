package app.pocketcp.core.adaptive

/** The app-level navigation chrome for a window (spec §5: bottom bar on phones, rail on tablets). */
enum class NavigationType { BottomBar, Rail }

/** Compact width gets a [NavigationType.BottomBar]; medium and expanded get a [NavigationType.Rail]. */
fun navigationTypeOf(widthClass: WindowWidthClass): NavigationType =
    when (widthClass) {
        WindowWidthClass.Compact -> NavigationType.BottomBar
        WindowWidthClass.Medium, WindowWidthClass.Expanded -> NavigationType.Rail
    }
