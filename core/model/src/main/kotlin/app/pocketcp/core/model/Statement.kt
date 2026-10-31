package app.pocketcp.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * A problem statement, judge-agnostic. These types are cached as JSON (ADR 0017): every @SerialName is
 * persisted, so never change or reuse one; add new names instead.
 */

/** The language a statement is shown in (spec §7.4). [key] is persisted: never change one. */
@Serializable
enum class StatementLanguage(
    val key: String,
) {
    @SerialName("en")
    En("en"),

    @SerialName("ru")
    Ru("ru"),
    ;

    companion object {
        /** The language with this [key], or `null` for an unknown key. */
        fun fromKey(key: String): StatementLanguage? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A problem's statement in one [language]. [timeLimitMillis] and [memoryLimitMegabytes] are `null` when the
 * judge states none; [inputFile] and [outputFile] are `null` for standard input and output.
 */
@Serializable
data class Statement(
    @SerialName("problem") val problem: ProblemId,
    @SerialName("language") val language: StatementLanguage,
    @SerialName("title") val title: String,
    @SerialName("time_limit_millis") val timeLimitMillis: Long?,
    @SerialName("memory_limit_megabytes") val memoryLimitMegabytes: Int?,
    @SerialName("input_file") val inputFile: String?,
    @SerialName("output_file") val outputFile: String?,
    @SerialName("sections") val sections: List<StatementSection>,
    @SerialName("samples") val samples: List<Sample>,
    @SerialName("materials") val materials: List<MaterialLink>,
)

/** One part of a statement, e.g. its legend or the input format. [title] is `null` when the judge shows none. */
@Serializable
data class StatementSection(
    @SerialName("kind") val kind: SectionKind,
    @SerialName("title") val title: String?,
    @SerialName("blocks") val blocks: List<StatementBlock>,
)

@Serializable
enum class SectionKind {
    @SerialName("legend")
    Legend,

    @SerialName("input")
    Input,

    @SerialName("output")
    Output,

    @SerialName("interaction")
    Interaction,

    @SerialName("scoring")
    Scoring,

    @SerialName("note")
    Note,

    @SerialName("other")
    Other,
}

/** A block of statement content. */
@Serializable
sealed interface StatementBlock {
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        @SerialName("inlines") val inlines: List<Inline>,
    ) : StatementBlock

    /** Math on its own line; [tex] is LaTeX without delimiters. */
    @Serializable
    @SerialName("display_math")
    data class DisplayMath(
        @SerialName("tex") val tex: String,
    ) : StatementBlock

    @Serializable
    @SerialName("image")
    data class Image(
        @SerialName("url") val url: String,
        @SerialName("description") val description: String?,
    ) : StatementBlock

    @Serializable
    @SerialName("table")
    data class Table(
        @SerialName("rows") val rows: List<TableRow>,
    ) : StatementBlock

    /** A list; each item is its own blocks, so items can hold nested lists. */
    @Serializable
    @SerialName("list")
    data class ListBlock(
        @SerialName("ordered") val ordered: Boolean,
        @SerialName("items") val items: List<List<StatementBlock>>,
    ) : StatementBlock

    /** Text shown verbatim in a monospace font, whitespace kept. */
    @Serializable
    @SerialName("preformatted")
    data class Preformatted(
        @SerialName("text") val text: String,
    ) : StatementBlock
}

@Serializable
data class TableRow(
    @SerialName("cells") val cells: List<TableCell>,
)

@Serializable
data class TableCell(
    @SerialName("inlines") val inlines: List<Inline>,
    @SerialName("header") val header: Boolean,
)

/** A run of content within a block. */
@Serializable
sealed interface Inline {
    @Serializable
    @SerialName("text")
    data class Text(
        @SerialName("text") val text: String,
        @SerialName("bold") val bold: Boolean = false,
        @SerialName("italic") val italic: Boolean = false,
        @SerialName("monospace") val monospace: Boolean = false,
    ) : Inline

    /** Math within a line; [tex] is LaTeX without delimiters. */
    @Serializable
    @SerialName("math")
    data class Math(
        @SerialName("tex") val tex: String,
    ) : Inline

    @Serializable
    @SerialName("link")
    data class Link(
        @SerialName("text") val text: String,
        @SerialName("url") val url: String,
    ) : Inline

    @Serializable
    @SerialName("line_break")
    data object LineBreak : Inline
}

/** A sample test, shown with copy and send-to-editor buttons (spec §7.4). */
@Serializable
data class Sample(
    @SerialName("input") val input: String,
    @SerialName("output") val output: String,
)

/** A link to the problem's editorial or other contest materials (spec §7.4). */
@Serializable
data class MaterialLink(
    @SerialName("title") val title: String,
    @SerialName("url") val url: String,
    @SerialName("kind") val kind: MaterialKind,
)

@Serializable
enum class MaterialKind {
    @SerialName("tutorial")
    Tutorial,

    @SerialName("announcement")
    Announcement,

    @SerialName("other")
    Other,
}
