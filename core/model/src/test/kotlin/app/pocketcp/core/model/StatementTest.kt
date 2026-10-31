package app.pocketcp.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementTest {
    private val json = Json

    /** Holds every block and inline type, nested where the model allows it, and both null and set optionals. */
    private val statement =
        Statement(
            problem = ProblemId(1900, "B1"),
            language = StatementLanguage.Ru,
            title = "B1. Пример",
            timeLimitMillis = 2_000,
            memoryLimitMegabytes = 256,
            inputFile = null,
            outputFile = "output.txt",
            sections =
                listOf(
                    StatementSection(
                        kind = SectionKind.Legend,
                        title = null,
                        blocks =
                            listOf(
                                StatementBlock.Paragraph(
                                    listOf(
                                        Inline.Text("Given "),
                                        Inline.Math("n \\le 10^5"),
                                        Inline.Text(" numbers", bold = true, italic = true, monospace = true),
                                        Inline.LineBreak,
                                        Inline.Link("see here", "https://codeforces.com/blog/entry/1"),
                                    ),
                                ),
                                StatementBlock.DisplayMath("\\sum_{i=1}^{n} a_i"),
                                StatementBlock.Image("https://espresso.codeforces.com/a.png", description = "a tree"),
                                StatementBlock.Image("https://espresso.codeforces.com/b.png", description = null),
                            ),
                    ),
                    StatementSection(
                        kind = SectionKind.Note,
                        title = "Note",
                        blocks =
                            listOf(
                                StatementBlock.Table(
                                    listOf(
                                        TableRow(listOf(TableCell(listOf(Inline.Text("n")), header = true))),
                                        TableRow(listOf(TableCell(listOf(Inline.Math("1")), header = false))),
                                    ),
                                ),
                                StatementBlock.ListBlock(
                                    ordered = true,
                                    items =
                                        listOf(
                                            listOf(StatementBlock.Paragraph(listOf(Inline.Text("first")))),
                                            listOf(
                                                StatementBlock.ListBlock(
                                                    ordered = false,
                                                    items = listOf(listOf(StatementBlock.Preformatted("  a\n b"))),
                                                ),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
            samples = listOf(Sample(input = "3\n1 2 3\n", output = "6\n")),
            materials =
                listOf(
                    MaterialLink("Tutorial", "https://codeforces.com/blog/entry/2", MaterialKind.Tutorial),
                    MaterialLink("Announcement", "https://codeforces.com/blog/entry/3", MaterialKind.Announcement),
                ),
        )

    @Test
    fun `a statement with every block and inline type round-trips through JSON`() {
        val encoded = json.encodeToString(Statement.serializer(), statement)

        assertEquals(statement, json.decodeFromString(Statement.serializer(), encoded))
    }

    @Test
    fun `the JSON uses the stable serial names, not Kotlin names`() {
        val encoded = json.encodeToString(Statement.serializer(), statement)

        listOf(
            "\"contest_id\":1900",
            "\"language\":\"ru\"",
            "\"time_limit_millis\":2000",
            "\"memory_limit_megabytes\":256",
            "\"input_file\":null",
            "\"output_file\":\"output.txt\"",
            "\"kind\":\"legend\"",
            "\"type\":\"display_math\"",
            "\"type\":\"line_break\"",
            "\"type\":\"list\"",
            "\"kind\":\"tutorial\"",
        ).forEach { assertTrue("missing $it in $encoded", it in encoded) }
    }

    @Test
    fun `fromKey maps both languages`() {
        assertEquals(StatementLanguage.En, StatementLanguage.fromKey("en"))
        assertEquals(StatementLanguage.Ru, StatementLanguage.fromKey("ru"))
    }

    @Test
    fun `fromKey of an unknown key is null`() {
        assertNull(StatementLanguage.fromKey("de"))
        assertNull(StatementLanguage.fromKey(""))
        assertNull(StatementLanguage.fromKey("En"))
    }
}
