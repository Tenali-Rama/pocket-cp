package app.pocketcp.core.statement.math

import kotlin.math.max

/** A root index (`\sqrt[3]{x}`) is set at script-script size, like TeX's `\scriptscriptstyle`. */
internal const val ROOT_INDEX_FONT_SCALE = 0.5f

// The index's baseline sits about half an em above the radical's baseline, tucked into the radical's left arm.
private const val ROOT_INDEX_RAISE_RATIO = 0.5f
private const val ROOT_INDEX_OVERLAP_RATIO = 0.55f

/** Places [index] over the left arm of [root], whose radical sign is [radicalWidth] wide. */
internal fun withRootIndex(
    root: LayoutBox,
    index: LayoutBox,
    radicalWidth: Float,
    fontSize: Float,
): LayoutBox {
    val overlap = radicalWidth * ROOT_INDEX_OVERLAP_RATIO
    val rootX = max(0f, index.width - overlap)
    val indexX = rootX + overlap - index.width
    val raise = fontSize * ROOT_INDEX_RAISE_RATIO
    val baseline = max(root.baseline, raise + index.baseline)
    val depth = max(root.height - root.baseline, index.height - index.baseline - raise)
    return LayoutBox(
        max(rootX + root.width, indexX + index.width),
        baseline + depth,
        baseline,
        translated(root.commands, rootX, baseline - root.baseline) +
            translated(index.commands, indexX, baseline - raise - index.baseline),
    )
}
