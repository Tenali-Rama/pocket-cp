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

/** One screenshot configuration: a [device], dark or light, and the font scale (1.5 for the large-font shot). */
data class ScreenshotConfig(
    val device: Device = Device.Phone,
    val dark: Boolean = true,
    val fontScale: Float = 1f,
) {
    /** `<name>_<device>_<theme>.png`; a variant such as AMOLED or large font goes into [name]. */
    fun fileName(name: String): String = "${name}_${device.id}_${if (dark) "dark" else "light"}.png"
}
