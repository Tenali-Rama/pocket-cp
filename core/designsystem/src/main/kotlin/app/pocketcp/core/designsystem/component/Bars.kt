package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.R

/**
 * The bar at the top of a screen: a one-line [title], a back arrow when [onNavigateBack] is set,
 * and [actions] (use [PcpIconButton]) at the end. Painted in `colors.background`, flat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcpTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = PocketCpTheme.colors
    TopAppBar(
        title = {
            Text(text = title, style = PocketCpTheme.typography.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                PcpIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.designsystem_navigate_back),
                    onClick = onNavigateBack,
                )
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.background,
                scrolledContainerColor = colors.background,
                titleContentColor = colors.onBackground,
                navigationIconContentColor = colors.onBackground,
                actionIconContentColor = colors.onBackground,
            ),
    )
}

/**
 * A toggleable filter such as a contest division. Selected chips show a check mark; TalkBack reads the
 * selected state from the chip itself, so the check is decorative. Touch target ≥ 48 dp.
 */
@Composable
fun PcpFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = PocketCpTheme.typography.label, maxLines = 1) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon =
            if (selected) {
                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
            } else {
                null
            },
        shape = PocketCpTheme.shapes.medium,
    )
}
