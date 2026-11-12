package app.pocketcp.core.problemview

import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.parser.CodeforcesStatementParser
import app.pocketcp.core.testing.Fixtures
import java.time.Instant

/** The problem every test shows: 2274A, recorded in `testdata/codeforces/pages/contest/2274/problem/`. */
internal val Paco = ProblemId(2274, "A")

private val FetchedAt: Instant = Instant.parse("2026-11-01T12:00:00Z")

/** 2274A's recorded page in [language], through the real Codeforces parser. */
internal fun recordedStatement(language: StatementLanguage = StatementLanguage.En): Statement =
    CodeforcesStatementParser.parse(
        Fixtures.text("codeforces/pages/contest/2274/problem/A.${language.key}.html"),
        Paco,
        language,
    )

/**
 * A hand-made statement in [language]: a legend of [paragraphs] paragraphs, so it can be longer than a window, then
 * one sample.
 */
internal fun shortStatement(
    language: StatementLanguage = StatementLanguage.En,
    paragraphs: Int = 1,
): Statement =
    Statement(
        problem = Paco,
        language = language,
        title = if (language == StatementLanguage.En) "A. Projections" else "A. Проекции",
        timeLimitMillis = 1_000,
        memoryLimitMegabytes = 256,
        inputFile = null,
        outputFile = null,
        sections =
            listOf(
                StatementSection(
                    SectionKind.Legend,
                    null,
                    List(paragraphs) { index ->
                        StatementBlock.Paragraph(listOf(Inline.Text("Paragraph ${index + 1} of the legend.")))
                    },
                ),
            ),
        samples = listOf(Sample("2\n1 2\n", "3\n")),
        materials = emptyList(),
    )

internal fun <T> cached(
    data: T?,
    refreshing: Boolean = false,
    error: JudgeError? = null,
): Cached<T> = Cached(data, fetchedAt = data?.let { FetchedAt }, refreshing = refreshing, error = error)
