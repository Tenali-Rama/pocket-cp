package app.pocketcp.core.datastore

private const val SMALL_TEXT_SCALE = 0.9f
private const val LARGE_TEXT_SCALE = 1.15f
private const val LARGEST_TEXT_SCALE = 1.3f

/**
 * The factor a text size multiplies text styles by, on top of the system font scale. One mapping for the app's UI
 * and code text sizes and for the statement's.
 */
val TextSizePreference.scale: Float
    get() =
        when (this) {
            TextSizePreference.Small -> SMALL_TEXT_SCALE
            TextSizePreference.Default -> 1f
            TextSizePreference.Large -> LARGE_TEXT_SCALE
            TextSizePreference.Largest -> LARGEST_TEXT_SCALE
        }
