package app.pocketcp.core.statement.math

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.statement.statementStyle

/** A replacement point for native TeX rendering in the next statement milestone. */
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

/** The statement's current math implementation. 045 replaces only the implementation behind this local. */
val LocalMathRenderer = staticCompositionLocalOf<MathRenderer> { SourceMathRenderer }

private object SourceMathRenderer : MathRenderer {
    @Composable
    override fun Inline(
        tex: String,
        textScale: Float,
    ) {
        Text(
            text = tex,
            style = statementStyle(PocketCpTheme.typography.code, textScale),
            color = PocketCpTheme.colors.onSurface,
            modifier =
                androidx.compose.ui.Modifier
                    .semantics { contentDescription = "math" },
        )
    }

    @Composable
    override fun Display(
        tex: String,
        textScale: Float,
    ) {
        Text(
            text = tex,
            style = statementStyle(PocketCpTheme.typography.code, textScale),
            color = PocketCpTheme.colors.onBackground,
            modifier =
                androidx.compose.ui.Modifier
                    .semantics { contentDescription = "math" },
        )
    }
}
