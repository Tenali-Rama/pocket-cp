package app.pocketcp.core.statement.math

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.statement.statementStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The room inline math takes in a line of text, in pixels. [baseline] is measured from the placeholder's top.
 *
 * The placeholder is centred on the text's centre ([PlaceholderVerticalAlign.TextCenter]), so it is made tall
 * enough on both sides of that centre for the content's baseline to land exactly on the text baseline.
 */
internal data class InlineMathMetrics(
    val width: Float,
    val height: Float,
    val baseline: Float,
) {
    companion object {
        /** [textCenter] is how far the surrounding text's centre sits above its baseline. */
        fun of(
            width: Float,
            ascent: Float,
            descent: Float,
            textCenter: Float,
        ): InlineMathMetrics {
            val halfHeight = max(ascent - textCenter, descent + textCenter)
            return InlineMathMetrics(ceil(width), halfHeight * 2, halfHeight + textCenter)
        }
    }
}

/** What an inline formula draws: its typeset layout, or its source when the typesetter rejects it. */
internal sealed interface InlineMath {
    val metrics: InlineMathMetrics

    data class Typeset(
        val layout: MathLayout,
        override val metrics: InlineMathMetrics,
    ) : InlineMath

    data class Source(
        val firstBaseline: Float,
        val style: TextStyle,
        override val metrics: InlineMathMetrics,
    ) : InlineMath
}

@Composable
internal fun rememberInlineMath(
    tex: String,
    textScale: Float,
): InlineMath {
    val textStyle = statementStyle(PocketCpTheme.typography.body, textScale)
    val fontSize = with(LocalDensity.current) { textStyle.fontSize.toPx() }
    val textCenter = remember(fontSize) { textCenterAboveBaseline(fontSize) }
    return when (val result = rememberTypeset(tex, textScale)) {
        is TexRenderResult.Ready -> {
            val layout = result.layout
            InlineMath.Typeset(
                layout,
                InlineMathMetrics.of(layout.width, layout.baseline, layout.height - layout.baseline, textCenter),
            )
        }
        is TexRenderResult.Fallback -> {
            val style = statementStyle(PocketCpTheme.typography.code, textScale)
            val measurer = rememberTextMeasurer()
            val measured = remember(tex, style, measurer) { measurer.measure(tex, style, softWrap = false) }
            InlineMath.Source(
                measured.firstBaseline,
                style,
                InlineMathMetrics.of(
                    measured.size.width.toFloat(),
                    measured.firstBaseline,
                    measured.size.height - measured.firstBaseline,
                    textCenter,
                ),
            )
        }
    }
}

/**
 * The text placeholder that reserves exactly the room this formula needs.
 *
 * Compose turns placeholder sp into px linearly (`density * fontScale`), unlike `toSp()`, which follows Android 14's
 * nonlinear font scaling, so the pixels are converted with that same linear factor.
 */
@Composable
internal fun InlineMath.placeholder(): Placeholder {
    val density = LocalDensity.current
    val pxPerSp = density.density * density.fontScale
    return Placeholder(
        width = (metrics.width / pxPerSp).sp,
        height = (metrics.height / pxPerSp).sp,
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
    )
}

/** Draws [math] inside its placeholder with the content baseline on the text baseline. */
@Composable
internal fun InlineMathContent(
    math: InlineMath,
    tex: String,
) {
    val color = PocketCpTheme.colors.onSurface
    Box(Modifier.fillMaxSize().semantics { contentDescription = tex }) {
        when (math) {
            is InlineMath.Typeset ->
                Canvas(Modifier.fillMaxSize()) {
                    translate(top = math.metrics.baseline - math.layout.baseline) {
                        drawIntoCanvas { canvas -> math.layout.draw(canvas.nativeCanvas, color.toArgb()) }
                    }
                }
            is InlineMath.Source ->
                Text(
                    text = tex,
                    style = math.style,
                    color = color,
                    softWrap = false,
                    modifier =
                        Modifier.offset {
                            IntOffset(0, (math.metrics.baseline - math.firstBaseline).roundToInt())
                        },
                )
        }
    }
}

/**
 * Compose centres a [PlaceholderVerticalAlign.TextCenter] placeholder on the span paint's integer font metrics.
 * Statement body text is the system sans-serif face, so the same metrics are read here.
 */
private fun textCenterAboveBaseline(fontSize: Float): Float {
    val metrics =
        Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                textSize = fontSize
                typeface = Typeface.SANS_SERIF
            }.fontMetricsInt
    return -(metrics.ascent + metrics.descent) / 2f
}
