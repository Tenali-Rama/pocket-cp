package app.pocketcp.core.statement.math

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.statement.statementStyle
import kotlin.math.max

/** A native renderer for the TeX subset used by Codeforces statements. */
@Immutable
interface MathRenderer {
    /** Renders TeX inside a line of statement text. */
    @Composable
    fun Inline(
        tex: String,
        textScale: Float,
    )

    /** Renders TeX as a statement block. */
    @Composable
    fun Display(
        tex: String,
        textScale: Float,
    )
}

/** The statement's default renderer. It never delegates math to a WebView. */
val LocalMathRenderer = staticCompositionLocalOf<MathRenderer> { NativeMathRenderer }

private object NativeMathRenderer : MathRenderer {
    @Composable
    override fun Inline(
        tex: String,
        textScale: Float,
    ) {
        val result = rememberTypeset(tex, textScale)
        when (result) {
            is TexRenderResult.Ready ->
                MathCanvas(
                    result.layout,
                    PocketCpTheme.colors.onSurface.toArgb(),
                    Modifier.fillMaxWidth().semantics { contentDescription = tex },
                )
            is TexRenderResult.Fallback -> SourceFallback(tex, textScale, Modifier.fillMaxWidth())
        }
    }

    @Composable
    override fun Display(
        tex: String,
        textScale: Float,
    ) {
        val result = rememberTypeset(tex, textScale)
        Box(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            when (result) {
                is TexRenderResult.Ready ->
                    MathCanvas(
                        result.layout,
                        PocketCpTheme.colors.onSurface.toArgb(),
                        Modifier.semantics { contentDescription = tex },
                    )
                is TexRenderResult.Fallback -> SourceFallback(tex, textScale)
            }
        }
    }
}

@Composable
private fun rememberTypeset(
    tex: String,
    textScale: Float,
): TexRenderResult {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fontSize = with(density) { statementStyle(PocketCpTheme.typography.body, textScale).fontSize.toPx() }
    val color = PocketCpTheme.colors.onSurface.toArgb()
    return NativeTexTypesetter.typeset(tex, fontSize, color)
}

@Composable
private fun MathCanvas(
    layout: MathLayout,
    color: Int,
    modifier: Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val width = with(density) { max(layout.width, 1f).toDp() }
    val height = with(density) { max(layout.height, 1f).toDp() }
    Canvas(modifier.requiredSize(width, height)) {
        drawIntoCanvas { canvas -> layout.draw(canvas.nativeCanvas, color) }
    }
}

@Composable
private fun SourceFallback(
    tex: String,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = tex,
        style = statementStyle(PocketCpTheme.typography.code, textScale),
        color = PocketCpTheme.colors.onSurface,
        softWrap = false,
        modifier = modifier.semantics { contentDescription = tex },
    )
}
