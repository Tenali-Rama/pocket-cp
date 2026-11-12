package app.pocketcp.core.problemview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.datastore.SettingsRepository
import app.pocketcp.core.model.ProblemId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Builds a [ProblemViewModel] for a problem (ADR 0012 addendum). Features inject this factory and call
 * `viewModel(key = problem.display) { factory.create(problem) }`, so a new view-model dependency never edits a
 * feature.
 */
class ProblemViewModelFactory
    @Inject
    constructor(
        private val statements: StatementRepository,
        private val settings: SettingsRepository,
        @ApplicationContext private val context: Context,
    ) {
        fun create(problem: ProblemId): ProblemViewModel {
            val openUrl: (String) -> Unit = ::openInBrowser
            return ProblemViewModel(problem, statements, settings, openUrl)
        }

        /** Opens [url] in the system browser (047 keeps Codeforces links in the app). */
        @Suppress("SwallowedException")
        private fun openInBrowser(url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (noBrowser: ActivityNotFoundException) {
                // No app handles the link (e.g. no browser on a locked-down device): nothing to open it with.
            }
        }
    }
