package app.pocketcp.core.problemview

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.datastore.scale
import app.pocketcp.core.designsystem.component.ErrorState
import app.pocketcp.core.designsystem.component.LoadingPlaceholder
import app.pocketcp.core.designsystem.component.PcpTopAppBar
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.statement.StatementContent
import app.pocketcp.core.ui.RefreshErrorBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Test tag of the pane's content under the top bar, in every state. */
const val PROBLEM_VIEW_TAG = "problem_view"

/**
 * The problem view's route: [ProblemScreen] on [viewModel]'s state. [onBack] shows a back arrow (a page on compact
 * width); `null` when the view is the detail pane beside a list.
 */
@Composable
fun ProblemPane(
    viewModel: ProblemViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    // A new problem in the same pane starts at its top.
    key(state.problem) {
        ProblemScreen(state, viewModel, onBack, modifier)
    }
}

/**
 * One problem's statement under a top bar (back arrow when [onBack] is set, `<id> · <title>`, the EN/RU toggle and
 * the reading menu). PageDown and PageUp scroll the statement by one viewport from wherever focus is in the view.
 */
@Composable
fun ProblemScreen(
    state: ProblemUiState,
    actions: ProblemActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val webPage = state.webPage
    if (webPage != null) {
        // Composed in place of the statement, so the statement's listState (and scroll) survives the page.
        WebPage(webPage, webPageTitle(state.statement, webPage), actions, modifier)
        return
    }
    val scope = rememberCoroutineScope()
    val loaded = state.status == ProblemStatus.Loaded
    val materials = state.statement?.materials.orEmpty()
    Column(modifier.onPreviewKeyEvent { event -> loaded && scrollByPage(event, listState, scope) }) {
        PcpTopAppBar(title = title(state.problem, state.statement), onNavigateBack = onBack) {
            LanguageToggle(state.language, actions::setLanguage)
            if (loaded && materials.isNotEmpty()) MaterialsMenu(materials, actions::openLink)
            ReadingMenu(state.textSize, state.width, actions)
        }
        Box(Modifier.weight(1f).fillMaxWidth().testTag(PROBLEM_VIEW_TAG)) {
            ProblemContent(state, actions, listState, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ProblemContent(
    state: ProblemUiState,
    actions: ProblemActions,
    listState: LazyListState,
    modifier: Modifier,
) {
    val statement = state.statement
    when {
        state.status == ProblemStatus.Loaded && statement != null -> {
            val maxWidth = state.width.maxWidth()
            StatementContent(
                statement = statement,
                textScale = state.textSize.scale,
                maxWidth = maxWidth,
                onOpenLink = actions::openLink,
                modifier = modifier,
                state = listState,
                header = {
                    state.error?.let { error ->
                        item(key = "refresh_error", contentType = "banner") {
                            RefreshErrorBanner(error, actions::retry, bannerModifier(maxWidth))
                        }
                    }
                },
            )
        }
        state.status == ProblemStatus.Error ->
            ErrorState(errorMessage(state.error), actions::retry, modifier)
        state.status == ProblemStatus.Unparsable && state.pageUrl != null ->
            StatementFallback(state.pageUrl, actions::openLink, modifier)
        state.status == ProblemStatus.Unparsable ->
            ErrorState(stringResource(R.string.problemview_unparsable), actions::retry, modifier)
        else -> LoadingPlaceholder(modifier)
    }
}

private fun bannerModifier(maxWidth: Dp): Modifier =
    if (maxWidth == Dp.Infinity) Modifier.fillMaxWidth() else Modifier.widthIn(max = maxWidth).fillMaxWidth()

/** `2274A · The Projections of Paco`: the id, then the statement's title without its `A. ` prefix. */
@Composable
private fun title(
    problem: ProblemId,
    statement: Statement?,
): String =
    if (statement == null) {
        problem.display
    } else {
        stringResource(R.string.problemview_title, problem.display, statementTitle(problem, statement))
    }

/** The statement's title without the judge's `<index>. ` prefix, which the top bar already shows in the id. */
internal fun statementTitle(
    problem: ProblemId,
    statement: Statement,
): String = statement.title.removePrefix("${problem.index}. ").trim()

/** No cached statement: why the refresh failed. A rejection shows the judge's own words. */
@Composable
private fun errorMessage(error: JudgeError?): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.problemview_error_offline)
        is JudgeError.RateLimited -> stringResource(R.string.problemview_error_rate_limited)
        is JudgeError.Rejected -> error.message.orEmpty()
        is JudgeError.Malformed -> stringResource(R.string.problemview_unparsable)
        is JudgeError.Unavailable, null -> stringResource(R.string.problemview_error_unavailable)
    }

/** PageDown / PageUp: scrolls [listState] by one viewport. Returns whether [event] was one of them. */
private fun scrollByPage(
    event: KeyEvent,
    listState: LazyListState,
    scope: CoroutineScope,
): Boolean {
    val direction =
        when (event.key) {
            Key.PageDown -> 1f
            Key.PageUp -> -1f
            else -> return false
        }
    if (event.type == KeyEventType.KeyDown) {
        val viewport = listState.layoutInfo.viewportSize.height
        scope.launch { listState.scrollBy(direction * viewport) }
    }
    return true
}
