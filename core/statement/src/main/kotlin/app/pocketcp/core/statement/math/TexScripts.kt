package app.pocketcp.core.statement.math

import kotlin.math.max

private const val SCRIPT_FONT_SCALE = 0.62f
private const val LIMIT_GAP_RATIO = 0.1f

// TeX-like script shifts, in ems of the base size: sup1 raises a superscript, sub1/sub2 drop a subscript
// (sub2 when a superscript is also present).
private const val SUPERSCRIPT_SHIFT_RATIO = 0.4f
private const val SUBSCRIPT_SHIFT_RATIO = 0.2f
private const val SUBSCRIPT_WITH_SUPERSCRIPT_SHIFT_RATIO = 0.25f
private const val SCRIPT_SPACE_RATIO = 0.03f

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
    // A plain letter's box: scripts move further out only by how much a taller or deeper base exceeds it.
    val letter = layoutNode(TexNode.Symbol("x"), fontSize)
    val boxes =
        ScriptBoxes(
            base = layoutNode(node.base, fontSize),
            subscript = node.subscript?.let { layoutNode(it, fontSize * SCRIPT_FONT_SCALE) },
            superscript = node.superscript?.let { layoutNode(it, fontSize * SCRIPT_FONT_SCALE) },
        )
    return if (isBigOperator(node)) {
        bigOperatorScriptsOrNull(boxes, fontSize) ?: regularScripts(boxes, letter, fontSize)
    } else {
        regularScripts(boxes, letter, fontSize)
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
    val gap = fontSize * LIMIT_GAP_RATIO
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
    letter: LayoutBox,
    fontSize: Float,
): LayoutBox {
    val base = boxes.base
    val sup = boxes.superscript
    val sub = boxes.subscript
    val baseDepth = base.height - base.baseline
    val supShift = fontSize * SUPERSCRIPT_SHIFT_RATIO + max(0f, base.baseline - letter.baseline)
    val subRatio = if (sup == null) SUBSCRIPT_SHIFT_RATIO else SUBSCRIPT_WITH_SUPERSCRIPT_SHIFT_RATIO
    val subShift = fontSize * subRatio + max(0f, baseDepth - (letter.height - letter.baseline))
    val baseline = max(base.baseline, sup?.let { supShift + it.baseline } ?: 0f)
    val depth =
        maxOf(
            baseDepth,
            sub?.let { subShift + it.height - it.baseline } ?: 0f,
            sup?.let { it.height - it.baseline - supShift } ?: 0f,
        )
    val scriptX = base.width + fontSize * SCRIPT_SPACE_RATIO
    val scriptWidth = max(sup?.width ?: 0f, sub?.width ?: 0f)
    val supCommands = sup?.let { translated(it.commands, scriptX, baseline - supShift - it.baseline) }.orEmpty()
    val subCommands = sub?.let { translated(it.commands, scriptX, baseline + subShift - it.baseline) }.orEmpty()
    return LayoutBox(
        scriptX + scriptWidth,
        baseline + depth,
        baseline,
        translated(base.commands, 0f, baseline - base.baseline) + supCommands + subCommands,
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
