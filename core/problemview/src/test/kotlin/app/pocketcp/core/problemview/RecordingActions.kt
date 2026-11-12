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

    companion object {
        const val RETRY = "retry"
    }
}
