@file:Suppress("MagicNumber") // This file is where the spacing and shape literals live.

package app.pocketcp.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How tightly rows and cards are packed (spec §15, Appearance). */
enum class UiDensity { Comfortable, Compact }

/**
 * Spacing steps on a 4 dp grid, plus [touchTarget], the minimum size of anything tappable (spec §16).
 * The `listItem*` and [cardVertical] tokens follow [UiDensity]; every other step is the same at both densities.
 *
 * @property listItemVertical top and bottom padding of a list, switch or choice row.
 * @property listItemHeight minimum height of a one-line row; never below [touchTarget].
 * @property listItemTwoLineHeight minimum height of a row with a supporting line; never below [touchTarget].
 * @property cardVertical top and bottom inset of text placed directly in a card (rows pad themselves).
 */
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
    val listItemVertical: Dp = 8.dp,
    val listItemHeight: Dp = 56.dp,
    val listItemTwoLineHeight: Dp = 72.dp,
    val cardVertical: Dp = 16.dp,
) {
    companion object {
        /** The spacing for [density]: [UiDensity.Compact] trims the vertical padding of rows and cards. */
        fun of(density: UiDensity): PocketCpSpacing =
            when (density) {
                UiDensity.Comfortable -> Comfortable
                UiDensity.Compact -> Compact
            }

        private val Comfortable = PocketCpSpacing()
        private val Compact =
            PocketCpSpacing(
                listItemVertical = 4.dp,
                listItemHeight = 48.dp,
                listItemTwoLineHeight = 48.dp,
                cardVertical = 8.dp,
            )
    }
}

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
