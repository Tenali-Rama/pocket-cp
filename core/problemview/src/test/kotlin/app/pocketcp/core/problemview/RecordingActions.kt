package app.pocketcp.core.problemview

import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.model.StatementLanguage

/** [ProblemActions] that record every call, for screen tests. */
class RecordingActions : ProblemActions {
    val calls = mutableListOf<Any>()

    override fun setLanguage(language: StatementLanguage) {
        calls += language
    }

    override fun setTextSize(size: TextSizePreference) {
        calls += size
    }

    override fun setWidth(width: StatementWidthPreference) {
        calls += width
    }

    override fun retry() {
        calls += RETRY
    }

    override fun openLink(url: String) {
        calls += url
    }

    override fun closeWebPage() {
        calls += CLOSE_WEB_PAGE
    }

    companion object {
        const val RETRY = "retry"
        const val CLOSE_WEB_PAGE = "closeWebPage"
    }
}
