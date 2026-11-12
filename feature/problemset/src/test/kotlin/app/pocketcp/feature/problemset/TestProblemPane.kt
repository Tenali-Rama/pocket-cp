package app.pocketcp.feature.problemset

import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.problemview.ProblemActions
import app.pocketcp.core.problemview.ProblemScreen
import app.pocketcp.core.problemview.ProblemStatus
import app.pocketcp.core.problemview.ProblemUiState

/** Statement actions that do nothing: screenshots and screen tests only look. */
private object NoProblemActions : ProblemActions {
    override fun setLanguage(language: StatementLanguage) = Unit

    override fun setTextSize(size: TextSizePreference) = Unit

    override fun setWidth(width: StatementWidthPreference) = Unit

    override fun retry() = Unit

    override fun openLink(url: String) = Unit
}

/**
 * The problem view as the screen's slot, on a short hand-made statement of [id], titled after its fixture problem
 * if it has one: `core/problemview`'s own screenshots cover the real statements, this only shows where the pane sits.
 */
internal val TestProblemPane: ProblemPaneSlot = { id, onBack, modifier ->
    val state = ProblemUiState(id, ProblemStatus.Loaded, testStatement(id))
    ProblemScreen(state, NoProblemActions, onBack, modifier)
}

private fun paragraph(vararg inlines: Inline) = StatementBlock.Paragraph(inlines.toList())

private fun testStatement(id: ProblemId): Statement {
    val name = FixtureProblems.find { it.id == id }?.name ?: "Maximum Subarray"
    return Statement(
        problem = id,
        language = StatementLanguage.En,
        title = "${id.index}. $name",
        timeLimitMillis = 2_000,
        memoryLimitMegabytes = 256,
        inputFile = null,
        outputFile = null,
        sections =
            listOf(
                StatementSection(
                    SectionKind.Legend,
                    null,
                    listOf(
                        paragraph(
                            Inline.Text("You are given an array "),
                            Inline.Math("a"),
                            Inline.Text(" of "),
                            Inline.Math("n"),
                            Inline.Text(" integers. Find the largest sum of a contiguous subarray."),
                        ),
                    ),
                ),
                StatementSection(
                    SectionKind.Input,
                    null,
                    listOf(
                        paragraph(
                            Inline.Text("The first line contains "),
                            Inline.Math("n"),
                            Inline.Text(" ("),
                            Inline.Math("1 \\le n \\le 2 \\cdot 10^5"),
                            Inline.Text("). The second line contains the array."),
                        ),
                    ),
                ),
                StatementSection(
                    SectionKind.Output,
                    null,
                    listOf(paragraph(Inline.Text("Print one integer: the largest sum."))),
                ),
            ),
        samples = listOf(Sample("5\n1 -2 3 4 -1\n", "7\n")),
        materials = emptyList(),
    )
}
