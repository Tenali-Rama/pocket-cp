package app.pocketcp.core.problemview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.webview.CodeforcesUrlPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the problem view asks for. */
interface ProblemActions {
    /** Shows statements in [language] from now on (a setting, spec §7.4). */
    fun setLanguage(language: StatementLanguage)

    /** The statement text size (a setting). */
    fun setTextSize(size: TextSizePreference)

    /** How wide statement text may run (a setting). */
    fun setWidth(width: StatementWidthPreference)

    /** A refresh the user asked for (Retry): always fetches. */
    fun retry()

    /**
     * Opens a link from the statement, a contest material or the WebView: a Codeforces page in the in-app WebView
     * over the statement, any other `http(s)` URL in the system browser. Anything else is dropped.
     */
    fun openLink(url: String)

    /** Closes the Codeforces page open over the statement. */
    fun closeWebPage()
}

/**
 * The problem view of [problem]: its statement from [statements] in the language of [settings], cache first, with
 * the reading settings. The state is read from construction on ([SharingStarted.Eagerly]), so a cached statement is
 * the first state the pane sees, never a skeleton. Links off Codeforces go to [openUrl].
 */
class ProblemViewModel(
    private val problem: ProblemId,
    private val statements: StatementRepository,
    private val settings: SettingsRepository,
    private val openUrl: (String) -> Unit,
) : ViewModel(),
    ProblemActions {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val statement =
        settings.settings
            .map { it.statementLanguage }
            .distinctUntilChanged()
            .flatMapLatest { language -> statements.statement(problem, language).map { language to it } }

    private val webPage = MutableStateFlow<String?>(null)

    val state: StateFlow<ProblemUiState> =
        combine(statement, settings.settings, webPage) { (language, cached), userSettings, page ->
            problemUiState(problem, language, cached, userSettings)
                .copy(pageUrl = statements.pageUrl(problem, language), webPage = page)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ProblemUiState(problem))

    override fun setLanguage(language: StatementLanguage) {
        viewModelScope.launch { settings.setStatementLanguage(language) }
    }

    override fun setTextSize(size: TextSizePreference) {
        viewModelScope.launch { settings.setStatementTextSize(size) }
    }

    override fun setWidth(width: StatementWidthPreference) {
        viewModelScope.launch { settings.setStatementWidth(width) }
    }

    override fun retry() {
        val language = state.value.language
        viewModelScope.launch { statements.refresh(problem, language, force = true) }
    }

    override fun openLink(url: String) {
        when {
            CodeforcesUrlPolicy.isAllowed(url) -> webPage.value = url
            isWebUrl(url) -> openUrl(url)
        }
    }

    override fun closeWebPage() {
        webPage.value = null
    }
}

/** `http` or `https`: what the system browser may open. `intent:`, `javascript:` and the like never leave the app. */
private fun isWebUrl(url: String): Boolean {
    val scheme = url.substringBefore(':').lowercase()
    return scheme == "https" || scheme == "http"
}
