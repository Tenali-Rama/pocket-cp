package app.pocketcp.core.data

import app.pocketcp.core.database.ContestEntity
import app.pocketcp.core.database.StatementEntity
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.model.TableCell
import app.pocketcp.core.model.TableRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MappersTest {
    @Test
    fun `contests round-trip through the entity, with and without a start`() {
        val round =
            Contest(
                ContestId(2000),
                "Codeforces Round 960",
                ContestPhase.Running,
                Instant.ofEpochSecond(1_721_000_000L),
                Duration.ofHours(2),
                false,
                ContestKind.Cf,
            )
        val gym = Contest(ContestId(100_001), "Training gym", ContestPhase.Finished, null, Duration.ofHours(5), true)

        listOf(round, gym).forEach { assertEquals(it, it.toEntity().toDomain()) }
        assertEquals(
            ContestEntity(2000, "Codeforces Round 960", "Running", 1_721_000_000L, 7_200L, false, "Cf"),
            round.toEntity(),
        )
    }

    @Test
    fun `every phase round-trips, and an unknown stored phase reads as Unknown`() {
        val base = ContestEntity(1, "Round 1", "Finished", null, 7_200L, false, "Cf")

        ContestPhase.entries.forEach { phase -> assertEquals(phase, base.copy(phase = phase.name).toDomain().phase) }
        assertEquals(ContestPhase.Unknown, base.copy(phase = "Paused").toDomain().phase)
    }

    @Test
    fun `every kind round-trips, and an unknown stored kind reads as Unknown`() {
        val base = Contest(ContestId(1), "Round 1", ContestPhase.Finished, null, Duration.ofHours(2), false)

        ContestKind.entries.forEach { kind ->
            val contest = base.copy(kind = kind)
            assertEquals(kind.name, contest.toEntity().kind)
            assertEquals(contest, contest.toEntity().toDomain())
        }
        val stored = base.toEntity().copy(kind = "Team")
        assertEquals(ContestKind.Unknown, stored.toDomain().kind)
    }

    @Test
    fun `problems round-trip through the entity, with and without optional fields`() {
        val rated = Problem(ProblemId(1234, "B1"), "Easy version", 1600, listOf("dp", "greedy"), 4_321)
        val unrated = Problem(ProblemId(4, "A"), "Watermelon", null, emptyList(), null)

        listOf(rated, unrated).forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `a statement with every block and inline type round-trips through the entity`() {
        val entity = everyType.toEntity(fetchedAtEpochMillis = 1_700_000_000_000L)

        assertEquals(StatementEntity(1900, "B1", "ru", entity.document, 1, 1_700_000_000_000L), entity)
        assertEquals(everyType, entity.toDomain())
    }

    @Test
    fun `a statement of another format version, or a corrupt one, reads as missing`() {
        val entity = everyType.toEntity(fetchedAtEpochMillis = 0L)

        assertNull(entity.copy(formatVersion = STATEMENT_FORMAT_VERSION + 1).toDomain())
        assertNull(entity.copy(formatVersion = 0).toDomain())
        assertNull(entity.copy(document = "{\"title\":").toDomain())
        assertNull(entity.copy(document = "{}").toDomain())
    }

    private companion object {
        /** Every block and inline type, nested where the model allows it, with null and set optionals. */
        val everyType =
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
                            SectionKind.Legend,
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
                                    StatementBlock.Image("https://espresso.codeforces.com/a.png", "a tree"),
                                    StatementBlock.Image("https://espresso.codeforces.com/b.png", null),
                                ),
                        ),
                        StatementSection(
                            SectionKind.Note,
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
                                                        items = listOf(listOf(StatementBlock.Preformatted(" a\n b"))),
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
    }
}
