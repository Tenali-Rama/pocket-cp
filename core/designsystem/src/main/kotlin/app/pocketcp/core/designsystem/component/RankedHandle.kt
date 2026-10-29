package app.pocketcp.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import app.pocketcp.core.designsystem.PocketCpColors
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.Rank

/**
 * A Codeforces handle in its [rank] colour, bold like on Codeforces. A legendary grandmaster's first letter
 * is drawn in the contrast colour (`colors.onBackground`: black in light, white in dark), the rest in red.
 */
@Composable
fun RankedHandle(
    handle: String,
    rank: Rank,
    modifier: Modifier = Modifier,
    style: TextStyle = PocketCpTheme.typography.body,
) {
    val colors = PocketCpTheme.colors
    val text = remember(handle, rank, colors) { rankedHandleText(handle, rank, colors) }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Shorthand for `RankedHandle(handle, Rank.ofRating(rating))`; `null` (never rated) is [Rank.Unrated]. */
@Composable
fun RankedHandle(
    handle: String,
    rating: Int?,
    modifier: Modifier = Modifier,
    style: TextStyle = PocketCpTheme.typography.body,
) {
    RankedHandle(handle = handle, rank = Rank.ofRating(rating), modifier = modifier, style = style)
}

/** The coloured text of a [RankedHandle]; split out so the legendary-grandmaster rule is unit-testable. */
internal fun rankedHandleText(
    handle: String,
    rank: Rank,
    colors: PocketCpColors,
): AnnotatedString {
    val rankColor = colors.rank.of(rank)
    return buildAnnotatedString {
        if (rank == Rank.LegendaryGrandmaster && handle.isNotEmpty()) {
            withStyle(SpanStyle(color = colors.onBackground)) { append(handle.take(1)) }
            withStyle(SpanStyle(color = rankColor)) { append(handle.drop(1)) }
        } else {
            withStyle(SpanStyle(color = rankColor)) { append(handle) }
        }
    }
}
