package app.pocketcp.core.statement.math

import kotlin.math.max

private const val SCRIPT_FONT_SCALE = 0.62f
private const val SCRIPT_GAP_RATIO = 0.1f

private data class ScriptBoxes(
    val base: LayoutBox,
    val subscript: LayoutBox?,
    val superscript: LayoutBox?,
)

internal fun layoutScripts(
    node: TexNode.Scripts,
    fontSize: Float,
    layoutNode: (TexNode, Float) -> LayoutBox,
): LayoutBox {
    val boxes =
        ScriptBoxes(
            base = layoutNode(node.base, fontSize),
            subscript = node.subscript?.let { layoutNode(it, fontSize * SCRIPT_FONT_SCALE) },
            superscript = node.superscript?.let { layoutNode(it, fontSize * SCRIPT_FONT_SCALE) },
        )
    return if (isBigOperator(node)) {
        bigOperatorScriptsOrNull(boxes, fontSize) ?: regularScripts(boxes, fontSize)
    } else {
        regularScripts(boxes, fontSize)
    }
}

private fun isBigOperator(node: TexNode.Scripts): Boolean =
    when (val base = node.base) {
        is TexNode.Symbol -> base.value == "∑" || base.value == "∏"
        else -> false
    }

private fun bigOperatorScriptsOrNull(
    boxes: ScriptBoxes,
    fontSize: Float,
): LayoutBox? {
    val sub = boxes.subscript
    val sup = boxes.superscript
    if (sub == null || sup == null) return null
    val gap = fontSize * SCRIPT_GAP_RATIO
    val width = max(boxes.base.width, max(sub.width, sup.width))
    val top = sup.height + gap
    val baseTop = top + boxes.base.height
    val subTop = baseTop + gap
    return LayoutBox(
        width,
        subTop + sub.height,
        baseTop + boxes.base.baseline,
        translated(sup.commands, (width - sup.width) / 2, 0f) +
            translated(boxes.base.commands, (width - boxes.base.width) / 2, top) +
            translated(sub.commands, (width - sub.width) / 2, subTop),
    )
}

private fun regularScripts(
    boxes: ScriptBoxes,
    fontSize: Float,
): LayoutBox {
    val gap = fontSize * SCRIPT_GAP_RATIO
    val baseTop = max(0f, boxes.base.baseline - (boxes.superscript?.height ?: 0f) + gap)
    val subTop = baseTop + boxes.base.baseline + gap
    val subBottom = boxes.subscript?.let { subTop + it.height } ?: 0f
    val bottom = max(baseTop + boxes.base.height, subBottom)
    val scriptWidth = max(boxes.superscript?.width ?: 0f, boxes.subscript?.width ?: 0f)
    val width = boxes.base.width + gap + scriptWidth
    val superscriptCommands =
        boxes.superscript
            ?.let {
                translated(it.commands, boxes.base.width + gap, max(0f, baseTop + boxes.base.baseline - it.height))
            }.orEmpty()
    val subscriptCommands = boxes.subscript?.let { translated(it.commands, boxes.base.width + gap, subTop) }.orEmpty()
    return LayoutBox(
        width,
        bottom,
        baseTop + boxes.base.baseline,
        translated(boxes.base.commands, 0f, baseTop) + superscriptCommands + subscriptCommands,
    )
}

internal fun translated(
    commands: List<DrawCommand>,
    x: Float,
    y: Float,
): List<DrawCommand> =
    commands.map { command ->
        when (command) {
            is DrawCommand.Text -> command.copy(x = command.x + x, baseline = command.baseline + y)
            is DrawCommand.Rule ->
                command.copy(
                    startX = command.startX + x,
                    endX = command.endX + x,
                    y = command.y + y,
                )
        }
    }
