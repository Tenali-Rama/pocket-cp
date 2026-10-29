@file:Suppress("MagicNumber") // This file is where the spacing and shape literals live.

package app.pocketcp.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing steps on a 4 dp grid, plus [touchTarget], the minimum size of anything tappable (spec §16). */
@Immutable
data class PocketCpSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val touchTarget: Dp = 48.dp,
)

/** Corner shapes, from chips and badges ([small]) to sheets and dialogs ([extraLarge]). */
@Immutable
data class PocketCpShapes(
    val small: CornerBasedShape = RoundedCornerShape(4.dp),
    val medium: CornerBasedShape = RoundedCornerShape(8.dp),
    val large: CornerBasedShape = RoundedCornerShape(12.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(16.dp),
)

internal fun PocketCpShapes.toMaterialShapes(): Shapes =
    Shapes(
        extraSmall = small,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
    )
