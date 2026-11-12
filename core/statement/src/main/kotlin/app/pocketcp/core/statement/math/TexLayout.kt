package app.pocketcp.core.statement.math

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

private const val MIN_GLYPH_WIDTH_RATIO = 0.12f
private const val SEQUENCE_GAP_RATIO = 0.045f
private const val FRACTION_FONT_SCALE = 0.78f
private const val FRACTION_PADDING_RATIO = 0.18f
private const val FRACTION_GAP_RATIO = 0.16f
private const val RULE_THICKNESS_RATIO = 0.06f

// TeX centres a fraction bar on the math axis, about a quarter of an em above the baseline.
private const val MATH_AXIS_RATIO = 0.25f
private const val RADICAL_FONT_SCALE = 1.12f
private const val RADICAL_GAP_RATIO = 0.08f
private const val RADICAL_OVERLAP_RATIO = 0.03f
private const val DELIMITER_FONT_SCALE = 1.25f
private const val MATRIX_FONT_SCALE = 0.82f
private const val MATRIX_CELL_GAP_RATIO = 0.3f
private const val MATRIX_ROW_GAP_RATIO = 0.2f
private const val MATRIX_DELIMITER_SCALE = 1.6f
private const val MATRIX_BASELINE_OFFSET_RATIO = 0.45f

/** Typeset formulas kept in memory: enough for a long statement at a couple of sizes and themes. */
internal const val MATH_CACHE_ENTRIES = 256

private val TextTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
private val CalligraphicTypeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
private val BoldTypeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
private val NormalTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)

private fun typefaceFor(style: SymbolStyle): Typeface =
    when (style) {
        SymbolStyle.Text -> TextTypeface
        SymbolStyle.Calligraphic -> CalligraphicTypeface
        SymbolStyle.Bold -> BoldTypeface
        SymbolStyle.Normal -> NormalTypeface
    }

internal sealed interface TexRenderResult {
    data class Ready(
        val layout: MathLayout,
    ) : TexRenderResult

    data class Fallback(
        val source: String,
    ) : TexRenderResult
}

internal data class MathLayout(
    val width: Float,
    val height: Float,
    val baseline: Float,
    val commands: List<DrawCommand>,
) {
    fun draw(
        canvas: Canvas,
        color: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        commands.forEach { command -> command.draw(canvas, paint) }
    }
}

internal sealed interface DrawCommand {
    fun draw(
        canvas: Canvas,
        paint: Paint,
    )

    data class Text(
        val value: String,
        val x: Float,
        val baseline: Float,
        val size: Float,
        val style: SymbolStyle,
    ) : DrawCommand {
        override fun draw(
            canvas: Canvas,
            paint: Paint,
        ) {
            paint.textSize = size
            paint.typeface = typefaceFor(style)
            canvas.drawText(value, x, baseline, paint)
        }
    }

    data class Rule(
        val startX: Float,
        val y: Float,
        val endX: Float,
        val thickness: Float,
    ) : DrawCommand {
        override fun draw(
            canvas: Canvas,
            paint: Paint,
        ) {
            paint.strokeWidth = thickness
            canvas.drawLine(startX, y, endX, y, paint)
        }
    }
}

internal data class MathCacheKey(
    val tex: String,
    val size: Float,
    val color: Int,
)

internal object NativeTexTypesetter {
    private val cache = LruCache<MathCacheKey, TexRenderResult>(MATH_CACHE_ENTRIES)
    private val typesetCount = AtomicInteger()

    fun typeset(
        tex: String,
        size: Float,
        color: Int,
    ): TexRenderResult {
        val key = MathCacheKey(tex, size, color)
        cache.get(key)?.let { return it }
        typesetCount.incrementAndGet()
        val result =
            runCatching { TexLayoutEngine(size).layout(TexParser(tex).parse()) }
                .fold(
                    onSuccess = { TexRenderResult.Ready(it) },
                    onFailure = { TexRenderResult.Fallback(tex) },
                )
        cache.put(key, result)
        return result
    }

    internal fun clearForTests() {
        cache.evictAll()
        typesetCount.set(0)
    }

    internal fun typesetCountForTests(): Int = typesetCount.get()
}

internal data class LayoutBox(
    val width: Float,
    val height: Float,
    val baseline: Float,
    val commands: List<DrawCommand>,
)

private class TexLayoutEngine(
    private val size: Float,
) {
    private val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun layout(node: TexNode): MathLayout {
        val box = layoutNode(node, size)
        return MathLayout(box.width, box.height, box.baseline, box.commands)
    }

    private fun layoutNode(
        node: TexNode,
        fontSize: Float,
    ): LayoutBox =
        when (node) {
            is TexNode.Sequence -> sequence(node.children, fontSize)
            is TexNode.Symbol -> glyph(node.value, fontSize, node.style)
            is TexNode.Text -> glyph(node.value, fontSize, SymbolStyle.Text)
            is TexNode.Fraction -> fraction(node.numerator, node.denominator, fontSize)
            is TexNode.Sqrt -> squareRoot(node, fontSize)
            is TexNode.Scripts -> layoutScripts(node, fontSize, ::layoutNode)
            is TexNode.Delimited -> delimited(node, fontSize)
            is TexNode.Binomial -> binomial(node, fontSize)
            is TexNode.Matrix -> matrix(node, fontSize)
        }

    private fun glyph(
        value: String,
        fontSize: Float,
        style: SymbolStyle,
    ): LayoutBox {
        measurePaint.textSize = fontSize
        measurePaint.typeface = typefaceFor(style)
        val metrics = measurePaint.fontMetrics
        return LayoutBox(
            width = max(measurePaint.measureText(value), fontSize * MIN_GLYPH_WIDTH_RATIO),
            height = metrics.descent - metrics.ascent,
            baseline = -metrics.ascent,
            commands = listOf(DrawCommand.Text(value, 0f, -metrics.ascent, fontSize, style)),
        )
    }

    private fun sequence(
        children: List<TexNode>,
        fontSize: Float,
    ): LayoutBox {
        if (children.isEmpty()) return glyph("", fontSize, SymbolStyle.Normal)
        val boxes = children.map { layoutNode(it, fontSize) }
        val gap = fontSize * SEQUENCE_GAP_RATIO
        val baseline = boxes.maxOf { it.baseline }
        val bottom = boxes.maxOf { it.height - it.baseline }
        var x = 0f
        val commands = mutableListOf<DrawCommand>()
        boxes.forEachIndexed { index, box ->
            commands += translated(box.commands, x, baseline - box.baseline)
            x += box.width
            if (index < boxes.lastIndex) x += gap
        }
        return LayoutBox(x, baseline + bottom, baseline, commands)
    }

    private fun fraction(
        numerator: TexNode,
        denominator: TexNode,
        fontSize: Float,
    ): LayoutBox {
        val num = layoutNode(numerator, fontSize * FRACTION_FONT_SCALE)
        val den = layoutNode(denominator, fontSize * FRACTION_FONT_SCALE)
        val horizontalPadding = fontSize * FRACTION_PADDING_RATIO
        val gap = fontSize * FRACTION_GAP_RATIO
        val rule = fontSize * RULE_THICKNESS_RATIO
        val width = max(num.width, den.width) + horizontalPadding * 2
        val top = num.height
        val denominatorTop = top + gap + rule + gap
        val baseline = top + gap + rule / 2 + fontSize * MATH_AXIS_RATIO
        return LayoutBox(
            width,
            denominatorTop + den.height,
            baseline,
            translated(num.commands, (width - num.width) / 2, 0f) +
                listOf(DrawCommand.Rule(0f, top + gap, width, rule)) +
                translated(den.commands, (width - den.width) / 2, denominatorTop),
        )
    }

    private fun squareRoot(
        node: TexNode.Sqrt,
        fontSize: Float,
    ): LayoutBox {
        val content = layoutNode(node.body, fontSize)
        val radical = glyph("√", fontSize * RADICAL_FONT_SCALE, SymbolStyle.Normal)
        val gap = fontSize * RADICAL_GAP_RATIO
        val top = max(0f, radical.baseline - content.baseline - fontSize * RADICAL_OVERLAP_RATIO)
        val baseline = top + content.baseline
        val width = radical.width + gap + content.width
        val root =
            LayoutBox(
                width,
                max(radical.height, top + content.height),
                baseline,
                translated(radical.commands, 0f, baseline - radical.baseline) +
                    translated(content.commands, radical.width + gap, top) +
                    listOf(DrawCommand.Rule(radical.width + gap, top, width, fontSize * RULE_THICKNESS_RATIO)),
            )
        val index = node.index ?: return root
        return withRootIndex(root, layoutNode(index, fontSize * ROOT_INDEX_FONT_SCALE), radical.width, fontSize)
    }

    private fun delimited(
        node: TexNode.Delimited,
        fontSize: Float,
    ): LayoutBox {
        val body = layoutNode(node.body, fontSize)
        val left = glyph(node.left, fontSize * DELIMITER_FONT_SCALE, SymbolStyle.Normal)
        val right = glyph(node.right, fontSize * DELIMITER_FONT_SCALE, SymbolStyle.Normal)
        val baseline = max(body.baseline, max(left.baseline, right.baseline))
        val bodyBottom = body.height - body.baseline
        val leftBottom = left.height - left.baseline
        val rightBottom = right.height - right.baseline
        val bottom = max(bodyBottom, max(leftBottom, rightBottom))
        val rightX = left.width + body.width
        return LayoutBox(
            left.width + body.width + right.width,
            baseline + bottom,
            baseline,
            translated(left.commands, 0f, baseline - left.baseline) +
                translated(body.commands, left.width, baseline - body.baseline) +
                translated(right.commands, rightX, baseline - right.baseline),
        )
    }

    private fun binomial(
        node: TexNode.Binomial,
        fontSize: Float,
    ): LayoutBox {
        val top = layoutNode(node.top, fontSize * FRACTION_FONT_SCALE)
        val bottom = layoutNode(node.bottom, fontSize * FRACTION_FONT_SCALE)
        val width = max(top.width, bottom.width)
        val gap = fontSize * MIN_GLYPH_WIDTH_RATIO
        val left = glyph("(", fontSize * DELIMITER_FONT_SCALE, SymbolStyle.Normal)
        val right = glyph(")", fontSize * DELIMITER_FONT_SCALE, SymbolStyle.Normal)
        val bottomY = top.height + gap
        val baseline = bottomY + bottom.baseline
        val contentX = left.width
        return LayoutBox(
            contentX + width + right.width,
            bottomY + bottom.height,
            baseline,
            translated(top.commands, contentX + (width - top.width) / 2, 0f) +
                translated(bottom.commands, contentX + (width - bottom.width) / 2, bottomY) +
                translated(left.commands, 0f, baseline - left.baseline) +
                translated(right.commands, contentX + width, baseline - right.baseline),
        )
    }

    private fun matrix(
        node: TexNode.Matrix,
        fontSize: Float,
    ): LayoutBox {
        val rows = node.rows.map { row -> row.map { layoutNode(it, fontSize * MATRIX_FONT_SCALE) } }
        val columnCount = rows.maxOfOrNull { it.size } ?: 1
        val columnWidths =
            (0 until columnCount).map { column ->
                rows.maxOfOrNull { it.getOrNull(column)?.width ?: 0f } ?: 0f
            }
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: fontSize }
        val cellGap = fontSize * MATRIX_CELL_GAP_RATIO
        val innerWidth = columnWidths.sum() + cellGap * max(0, columnCount - 1)
        val innerHeight = rowHeights.sum() + fontSize * MATRIX_ROW_GAP_RATIO * max(0, rows.size - 1)
        val delimiterSize = fontSize * MATRIX_DELIMITER_SCALE
        val left = delimiterOrNull(node.style.left, delimiterSize)
        val right = delimiterOrNull(node.style.right, delimiterSize)
        val leftWidth = left?.width ?: 0f
        val baseline = innerHeight / 2 + fontSize * MATRIX_BASELINE_OFFSET_RATIO
        val commands = mutableListOf<DrawCommand>()
        var y = 0f
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { column, cell ->
                val slack = if (node.style.leftAligned) 0f else (columnWidths[column] - cell.width) / 2
                val x = columnWidths.take(column).sum() + cellGap * column
                commands += translated(cell.commands, leftWidth + x + slack, y)
            }
            y += rowHeights[rowIndex] + fontSize * MATRIX_ROW_GAP_RATIO
        }
        left?.let { commands += translated(it.commands, 0f, baseline - it.baseline) }
        right?.let { commands += translated(it.commands, leftWidth + innerWidth, baseline - it.baseline) }
        return LayoutBox(leftWidth + innerWidth + (right?.width ?: 0f), innerHeight, baseline, commands)
    }

    private fun delimiterOrNull(
        value: String,
        fontSize: Float,
    ): LayoutBox? = if (value.isEmpty()) null else glyph(value, fontSize, SymbolStyle.Normal)
}
