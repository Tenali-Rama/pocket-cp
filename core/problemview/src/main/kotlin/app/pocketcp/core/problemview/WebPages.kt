package app.pocketcp.core.problemview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpIconButton
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.Statement
import app.pocketcp.core.webview.CodeforcesWebView

/** Test tag of the WebView fallback for an unparsable statement: the notice and the page under it. */
const val PROBLEM_FALLBACK_TAG = "problem_fallback"

/** Test tag of the contest materials button; its items are `problem_material_<n>`, from 1. */
const val MATERIALS_MENU_TAG = "problem_materials"

/** Test tag of the WebView of a Codeforces page open over the statement. */
const val WEB_PAGE_TAG = "problem_web_page"

/**
 * The statement's page on Codeforces, for a statement the parser can't read (spec §18): a one-line notice, then the
 * WebView filling the pane (Codeforces lays the page out itself, so no reading column).
 */
@Composable
internal fun StatementFallback(
    url: String,
    onExternalLink: (String) -> Unit,
    modifier: Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(modifier.testTag(PROBLEM_FALLBACK_TAG)) {
        Text(
            text = stringResource(R.string.problemview_fallback_notice),
            style = PocketCpTheme.typography.label,
            color = PocketCpTheme.colors.onSurfaceMuted,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        HorizontalDivider(color = PocketCpTheme.colors.outline)
        CodeforcesWebView(url, onExternalLink, Modifier.weight(1f).fillMaxWidth())
    }
}

/** A Codeforces page over the statement: a top bar with [title] and a back arrow; back (arrow or system) closes it. */
@Composable
internal fun WebPage(
    url: String,
    title: String,
    actions: ProblemActions,
    modifier: Modifier,
) {
    BackHandler(onBack = actions::closeWebPage)
    Column(modifier) {
        PcpTopAppBar(title = title, onNavigateBack = actions::closeWebPage)
        CodeforcesWebView(url, actions::openLink, Modifier.weight(1f).fillMaxWidth().testTag(WEB_PAGE_TAG))
    }
}

/** The open page's title: the contest material's own name when it is one, else "Codeforces". */
@Composable
internal fun webPageTitle(
    statement: Statement?,
    url: String,
): String {
    val material = statement?.materials?.firstOrNull { it.url == url }
    return material?.title ?: stringResource(R.string.problemview_web_page_title)
}

/** The "Contest materials" button: a menu of [materials] (announcement, tutorial…); a choice opens it. */
@Composable
internal fun MaterialsMenu(
    materials: List<MaterialLink>,
    onOpen: (String) -> Unit,
) {
    val colors = PocketCpTheme.colors
    val typography = PocketCpTheme.typography
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        PcpIconButton(
            icon = Icons.AutoMirrored.Filled.List,
            contentDescription = stringResource(R.string.problemview_materials),
            onClick = { expanded = true },
            modifier = Modifier.testTag(MATERIALS_MENU_TAG),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceRaised,
        ) {
            materials.forEachIndexed { index, material ->
                DropdownMenuItem(
                    text = { Text(material.title, style = typography.body, color = colors.onSurface) },
                    onClick = {
                        expanded = false
                        onOpen(material.url)
                    },
                    modifier = Modifier.testTag("problem_material_${index + 1}"),
                )
            }
        }
    }
}
