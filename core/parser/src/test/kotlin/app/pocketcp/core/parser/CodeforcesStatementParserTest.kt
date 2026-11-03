package app.pocketcp.core.parser

import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CodeforcesStatementParserTest {
    @Test
    fun `header strips prefix and maps limits and standard io`() {
        val statement = parseFixture("2276", "F", "en")

        assertEquals("Permutation Coloring Game", statement.title)
        assertEquals(3_000L, statement.timeLimitMillis)
        assertEquals(512, statement.memoryLimitMegabytes)
        assertNull(statement.inputFile)
        assertNull(statement.outputFile)
    }

    @Test
    fun `section kinds are structural and stable across languages`() {
        val english = parseFixture("2273", "D", "en")
        val russian = parseFixture("2273", "D", "ru")

        val expected = listOf(SectionKind.Legend, SectionKind.Input, SectionKind.Interaction, SectionKind.Note)
        assertEquals(expected, english.sections.map { it.kind })
        assertEquals(expected, russian.sections.map { it.kind })
    }

    @Test
    fun `output section class is stable across languages`() {
        val english = parseFixture("2274", "A", "en")
        val russian = parseFixture("2274", "A", "ru")

        val expected = listOf(SectionKind.Legend, SectionKind.Input, SectionKind.Output, SectionKind.Note)
        assertEquals(expected, english.sections.map { it.kind })
        assertEquals(expected, russian.sections.map { it.kind })
    }

    @Test
    fun `math styles images lists and samples are mapped`() {
        val statement = parseFixture("2274", "A", "en")
        val blocks = statement.sections.flatMap { it.blocks }
        val listed = parseFixture("2276", "F", "en").sections.flatMap { it.blocks }

        assertTrue(blocks.any { it is StatementBlock.Image && it.url.startsWith("https://") })
        assertTrue(listed.any { it is StatementBlock.ListBlock })
        assertTrue(
            blocks.any { block ->
                block is StatementBlock.Paragraph && block.inlines.any { it is Inline.Math }
            },
        )
        assertTrue(statement.samples.isNotEmpty())
        assertEquals("5\n1\n3\n2\n2 5\n3\n2 7 1\n4\n-3 0 -3 0\n1\n0", statement.samples.first().input)
    }

    @Test
    fun `interactive fixture contains display math and inline styles`() {
        val statement = parseFixture("2273", "D", "en")
        val blocks = statement.sections.flatMap { it.blocks }
        val delimiter = "\$".repeat(6)
        val display =
            CodeforcesStatementParser.parse(
                """
                <div class="problem-statement"><div class="header"><div class="title">A. Display</div></div>
                <div><p>${delimiter}x &lt; y$delimiter</p></div></div>
                """.trimIndent(),
                ProblemId(1, "A"),
                StatementLanguage.En,
            )
        assertTrue(display.sections.flatMap { it.blocks }.any { it is StatementBlock.DisplayMath })
        assertTrue(
            blocks
                .flatMap { block ->
                    (block as? StatementBlock.Paragraph)?.inlines.orEmpty()
                }.any { it is Inline.Text && it.italic },
        )
        assertTrue(
            blocks
                .flatMap { block ->
                    (block as? StatementBlock.Paragraph)?.inlines.orEmpty()
                }.any { it is Inline.Text && it.bold },
        )
    }

    @Test
    fun `materials resolve urls and classify localized names`() {
        val statement = parseFixture("2276", "F", "en")

        assertEquals(listOf("Announcement", "Tutorial"), statement.materials.map { it.title })
        assertEquals(listOf(MaterialKind.Announcement, MaterialKind.Tutorial), statement.materials.map { it.kind })
        assertTrue(statement.materials.all { it.url.startsWith("https://codeforces.com/") })

        val localized =
            CodeforcesStatementParser.parse(
                """
                <html><body>
                  <div class="problem-statement"><div class="header"><div class="title">A. Test</div></div></div>
                  <div class="roundbox sidebox sidebar-menu">
                    <div class="caption titled">Материалы конкурса</div>
                    <a href="/blog/entry/1">Разбор</a><a href="/blog/entry/2">Анонс</a>
                  </div>
                </body></html>
                """.trimIndent(),
                ProblemId(1, "A"),
                StatementLanguage.Ru,
            )
        assertEquals(listOf(MaterialKind.Tutorial, MaterialKind.Announcement), localized.materials.map { it.kind })
    }

    @Test
    fun `table and old pre samples preserve their shape`() {
        val inline = "\$\$\$"
        val display = inline + inline
        val html =
            """
            <html><body><div class="problem-statement">
              <div class="header"><div class="title">A. Synthetic</div></div>
              <div class="legend">
                <p>${inline}a &lt; b$inline <span class="tex-font-style-bf">bold</span></p>
                <p>${display}x+y$display</p>
                <img src="//example.com/diagram.png" alt="diagram">
                <table><tr><th>Header</th><td>Value</td></tr></table>
                <ol><li>first<ul><li>nested</li></ul></li></ol>
              </div>
              <div class="sample-tests"><div class="sample-test">
                <div class="input"><pre>one<br>two<br></pre></div>
                <div class="output"><pre>ok<br></pre></div>
              </div></div>
            </div></body></html>
            """.trimIndent()

        val statement = CodeforcesStatementParser.parse(html, ProblemId(1, "A"), StatementLanguage.En)
        val blocks = statement.sections.single().blocks
        val table = blocks.filterIsInstance<StatementBlock.Table>().single()
        assertTrue(
            table.rows
                .single()
                .cells
                .first()
                .header,
        )
        assertEquals("https://example.com/diagram.png", blocks.filterIsInstance<StatementBlock.Image>().single().url)
        assertTrue(
            blocks.filterIsInstance<StatementBlock.ListBlock>().single().items.single().any {
                it is StatementBlock.ListBlock
            },
        )
        assertEquals("x+y", blocks.filterIsInstance<StatementBlock.DisplayMath>().single().tex)
        assertEquals("one\ntwo", statement.samples.single().input)
        assertEquals("ok", statement.samples.single().output)
    }

    @Test
    fun `blocked and unknown pages use only parser errors`() {
        assertThrows<StatementParseError.Blocked> {
            CodeforcesStatementParser.parse(
                Fixtures.text("codeforces/pages/error.cloudflare.html"),
                ProblemId(1, "A"),
                StatementLanguage.En,
            )
        }
        assertThrows<StatementParseError.UnknownMarkup> {
            CodeforcesStatementParser.parse(
                "<html><body><div>contest</div></body></html>",
                ProblemId(1, "A"),
                StatementLanguage.En,
            )
        }
        assertThrows<StatementParseError.UnknownMarkup> {
            CodeforcesStatementParser.parse(
                "<div class=\"problem-statement\"><div class=\"header\"></div></div>",
                ProblemId(1, "A"),
                StatementLanguage.En,
            )
        }
    }

    @Test
    fun `truncated fixtures either parse or throw the typed error`() {
        val fixtures =
            listOf(
                "2276/problem/F.en.html" to StatementLanguage.En,
                "2276/problem/F.ru.html" to StatementLanguage.Ru,
                "2274/problem/A.en.html" to StatementLanguage.En,
                "2274/problem/A.ru.html" to StatementLanguage.Ru,
                "2273/problem/D.en.html" to StatementLanguage.En,
                "2273/problem/D.ru.html" to StatementLanguage.Ru,
            )
        fixtures.forEachIndexed { index, (path, language) ->
            val html = Fixtures.text("codeforces/pages/contest/$path")
            val midpoint = html.length / 2
            try {
                CodeforcesStatementParser.parse(html.take(midpoint), ProblemId(index + 1, "A"), language)
            } catch (_: StatementParseError) {
            } catch (error: Throwable) {
                fail("Unexpected parser failure for $path: $error")
            }
        }
    }

    private fun parseFixture(
        contest: String,
        index: String,
        language: String,
    ) = CodeforcesStatementParser.parse(
        Fixtures.text("codeforces/pages/contest/$contest/problem/$index.$language.html"),
        ProblemId(contest.toInt(), index),
        StatementLanguage.fromKey(language) ?: error("Unknown test language: $language"),
    )

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.simpleName}")
        } catch (error: Throwable) {
            if (error !is T) throw error
        }
    }
}
