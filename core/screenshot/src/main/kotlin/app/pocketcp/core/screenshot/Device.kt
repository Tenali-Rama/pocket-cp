package app.pocketcp.core.screenshot

/**
 * A window size in the screenshot matrix (QUALITY.md §4, docs/decisions/0003-screenshot-matrix.md).
 * Rendered at density 1, so a golden is [widthDp] × [heightDp] pixels.
 */
enum class Device(
    val id: String,
    val widthDp: Int,
    val heightDp: Int,
) {
    Phone(id = "phone", widthDp = 411, heightDp = 914),
    Tablet(id = "tablet", widthDp = 1280, heightDp = 800),
    Foldable(id = "foldable", widthDp = 673, heightDp = 841),
    Desktop(id = "desktop", widthDp = 1440, heightDp = 900),
}
