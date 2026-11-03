package app.pocketcp.core.statement.math

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

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
    private val commands: List<DrawCommand>,
) {
    fun draw(
        canvas: Canvas,
        color: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        commands.forEach { command -> command.draw(canvas, paint) }
    }
}

private sealed interface DrawCommand {
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
            paint.typeface =
                when (style) {
                    SymbolStyle.Text -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    SymbolStyle.Calligraphic -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    SymbolStyle.Normal -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
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
    private val cache = ConcurrentHashMap<MathCacheKey, TexRenderResult>()
    private val typesetCount = AtomicInteger()

    fun typeset(
        tex: String,
        size: Float,
        color: Int,
    ): TexRenderResult {
        val key = MathCacheKey(tex, size, color)
        return cache.getOrPut(key) {
            typesetCount.incrementAndGet()
            runCatching { TexLayoutEngine(size).layout(TexParser(tex).parse()) }
                .fold(
                    onSuccess = { TexRenderResult.Ready(it) },
                    onFailure = { TexRenderResult.Fallback(tex) },
                )
        }
    }

    internal fun clearForTests() {
        cache.clear()
        typesetCount.set(0)
    }

    internal fun typesetCountForTests(): Int = typesetCount.get()
}

private data class LayoutBox(
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
            is TexNode.Sqrt -> squareRoot(node.body, fontSize)
            is TexNode.Scripts -> scripts(node, fontSize)
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
        measurePaint.typeface =
            when (style) {
                SymbolStyle.Text -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                SymbolStyle.Calligraphic -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                SymbolStyle.Normal -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
        val metrics = measurePaint.fontMetrics
        return LayoutBox(
            width = max(measurePaint.measureText(value), fontSize * 0.12f),
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
        val gap = fontSize * 0.045f
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
        val num = layoutNode(numerator, fontSize * 0.78f)
        val den = layoutNode(denominator, fontSize * 0.78f)
        val horizontalPadding = fontSize * 0.18f
        val gap = fontSize * 0.16f
        val rule = fontSize * 0.06f
        val width = max(num.width, den.width) + horizontalPadding * 2
        val top = num.height
        val denominatorTop = top + gap + rule + gap
        val baseline = denominatorTop + den.baseline
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
        body: TexNode,
        fontSize: Float,
    ): LayoutBox {
        val content = layoutNode(body, fontSize)
        val radical = glyph("√", fontSize * 1.12f, SymbolStyle.Normal)
        val gap = fontSize * 0.08f
        val top = max(0f, radical.baseline - content.baseline - fontSize * 0.03f)
        val baseline = top + content.baseline
        val width = radical.width + gap + content.width
        return LayoutBox(
            width,
            max(radical.height, top + content.height),
            baseline,
            translated(radical.commands, 0f, baseline - radical.baseline) +
                translated(content.commands, radical.width + gap, top) +
                listOf(DrawCommand.Rule(radical.width + gap, top, width, fontSize * 0.06f)),
        )
    }

    private fun scripts(
        node: TexNode.Scripts,
        fontSize: Float,
    ): LayoutBox {
        val base = layoutNode(node.base, fontSize)
        val sub = node.subscript?.let { layoutNode(it, fontSize * 0.62f) }
        val sup = node.superscript?.let { layoutNode(it, fontSize * 0.62f) }
        val gap = fontSize * 0.1f
        val isBigOperator = node.base is TexNode.Symbol && (node.base.value == "∑" || node.base.value == "∏")
        return if (isBigOperator && sub != null && sup != null) {
            val width = max(base.width, max(sub.width, sup.width))
            val top = sup.height + gap
            val baseTop = top + base.height
            val subTop = baseTop + gap
            LayoutBox(
                width,
                subTop + sub.height,
                baseTop + base.baseline,
                translated(sup.commands, (width - sup.width) / 2, 0f) +
                    translated(base.commands, (width - base.width) / 2, top) +
                    translated(sub.commands, (width - sub.width) / 2, subTop),
            )
        } else {
            val top = max(0f, base.baseline - (sup?.height ?: 0f) + gap)
            val baseTop = top
            val subTop = baseTop + base.baseline + gap
            val bottom = max(baseTop + base.height, sub?.let { subTop + it.height } ?: 0f)
            val width = base.width + gap + max(sup?.width ?: 0f, sub?.width ?: 0f)
            LayoutBox(
                width,
                bottom,
                baseTop + base.baseline,
                translated(base.commands, 0f, baseTop) +
                    (
                        sup?.let { translated(it.commands, base.width + gap, max(0f, baseTop + base.baseline - it.height)) }
                            ?: emptyList()
                    ) +
                    (sub?.let { translated(it.commands, base.width + gap, subTop) } ?: emptyList()),
            )
        }
    }

    private fun delimited(
        node: TexNode.Delimited,
        fontSize: Float,
    ): LayoutBox {
        val body = layoutNode(node.body, fontSize)
        val left = glyph(node.left, fontSize * 1.25f, SymbolStyle.Normal)
        val right = glyph(node.right, fontSize * 1.25f, SymbolStyle.Normal)
        val baseline = max(body.baseline, max(left.baseline, right.baseline))
        val bottom = max(body.height - body.baseline, max(left.height - left.baseline, right.height - right.baseline))
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
        val top = layoutNode(node.top, fontSize * 0.78f)
        val bottom = layoutNode(node.bottom, fontSize * 0.78f)
        val width = max(top.width, bottom.width)
        val gap = fontSize * 0.12f
        val left = glyph("(", fontSize * 1.25f, SymbolStyle.Normal)
        val right = glyph(")", fontSize * 1.25f, SymbolStyle.Normal)
        val topY = 0f
        val bottomY = top.height + gap
        val baseline = bottomY + bottom.baseline
        val contentX = left.width
        return LayoutBox(
            contentX + width + right.width,
            bottomY + bottom.height,
            baseline,
            translated(top.commands, contentX + (width - top.width) / 2, topY) +
                translated(bottom.commands, contentX + (width - bottom.width) / 2, bottomY) +
                translated(left.commands, 0f, baseline - left.baseline) +
                translated(right.commands, contentX + width, baseline - right.baseline),
        )
    }

    private fun matrix(
        node: TexNode.Matrix,
        fontSize: Float,
    ): LayoutBox {
        val rows = node.rows.map { row -> row.map { layoutNode(it, fontSize * 0.82f) } }
        val columnCount = rows.maxOfOrNull { it.size } ?: 1
        val columnWidths = (0 until columnCount).map { column -> rows.maxOfOrNull { it.getOrNull(column)?.width ?: 0f } ?: 0f }
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: fontSize }
        val cellGap = fontSize * 0.3f
        val innerWidth = columnWidths.sum() + cellGap * max(0, columnCount - 1)
        val innerHeight = rowHeights.sum() + fontSize * 0.2f * max(0, rows.size - 1)
        val left = glyph(if (node.cases) "{" else "(", fontSize * 1.6f, SymbolStyle.Normal)
        val right = glyph(if (node.cases) "" else ")", fontSize * 1.6f, SymbolStyle.Normal)
        val baseline = innerHeight / 2 + fontSize * 0.45f
        val commands = mutableListOf<DrawCommand>()
        var y = 0f
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { column, cell ->
                val x = columnWidths.take(column).sum() + cellGap * column
                commands += translated(cell.commands, left.width + x + (columnWidths[column] - cell.width) / 2, y)
            }
            y += rowHeights[rowIndex] + fontSize * 0.2f
        }
        commands += translated(left.commands, 0f, baseline - left.baseline)
        if (!node.cases) commands += translated(right.commands, left.width + innerWidth, baseline - right.baseline)
        val width = left.width + innerWidth + if (node.cases) 0f else right.width
        return LayoutBox(width, innerHeight, baseline, commands)
    }

    private fun translated(
        commands: List<DrawCommand>,
        x: Float,
        y: Float,
    ): List<DrawCommand> =
        commands.map { command ->
            when (command) {
                is DrawCommand.Text -> command.copy(x = command.x + x, baseline = command.baseline + y)
                is DrawCommand.Rule -> command.copy(startX = command.startX + x, endX = command.endX + x, y = command.y + y)
            }
        }
}
