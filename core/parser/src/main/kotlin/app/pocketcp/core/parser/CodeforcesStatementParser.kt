package app.pocketcp.core.parser

import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.model.TableCell
import app.pocketcp.core.model.TableRow
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.Locale

/** Maps the supported Codeforces problem-statement markup to the domain statement tree. */
@Suppress("TooManyFunctions")
object CodeforcesStatementParser {
    private const val BASE_URL = "https://codeforces.com"
    private val whitespace = Regex("\\s+")
    private val math =
        Regex(
            "\\${'$'}{6}(.*?)\\${'$'}{6}|\\${'$'}{3}(.*?)\\${'$'}{3}",
            RegexOption.DOT_MATCHES_ALL,
        )

    /** Parses one Codeforces statement, exposing only [StatementParseError] on failure. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ThrowsCount")
    fun parse(
        html: String,
        problem: ProblemId,
        language: StatementLanguage,
    ): Statement {
        try {
            if (isCloudflareChallenge(html)) throw StatementParseError.Blocked
            val document = Jsoup.parse(html, BASE_URL)
            val root =
                document.selectFirst(".problem-statement")
                    ?: throw StatementParseError.UnknownMarkup("Missing .problem-statement")
            val header =
                root.selectFirst(".header")
                    ?: throw StatementParseError.UnknownMarkup("Missing statement header")
            val title =
                header
                    ?.selectFirst(".title")
                    ?.text()
                    ?.trim()
                    ?.removeProblemPrefix()
                    ?: throw StatementParseError.UnknownMarkup("Missing statement title")
            if (title.isEmpty()) throw StatementParseError.UnknownMarkup("Empty statement title")

            return Statement(
                problem = problem,
                language = language,
                title = title,
                timeLimitMillis = header.selectFirst(".time-limit")?.text()?.toMillis(),
                memoryLimitMegabytes = header.selectFirst(".memory-limit")?.text()?.toMegabytes(),
                inputFile = header.fileName(".input-file"),
                outputFile = header.fileName(".output-file"),
                sections = parseSections(root),
                samples = parseSamples(root),
                materials = parseMaterials(document),
            )
        } catch (error: StatementParseError) {
            throw error
        } catch (error: Exception) {
            throw StatementParseError.UnknownMarkup(error.message ?: "Unsupported statement markup")
        }
    }

    private fun isCloudflareChallenge(html: String): Boolean {
        val lower = html.lowercase(Locale.ROOT)
        if ("problem-statement" in lower) return false
        return "cf-chl-" in lower ||
            "challenge-platform" in lower ||
            "just a moment" in lower ||
            "checking your browser" in lower
    }

    private fun parseSections(root: Element): List<StatementSection> {
        var hasLegend = false
        return root.children().flatMap { child ->
            if (child.hasClass("header") || child.hasClass("sample-tests") || child.hasClass("contest-materials")) {
                return@flatMap emptyList()
            }
            val kind = child.sectionKind(hasLegend)
            if (kind == SectionKind.Legend) hasLegend = true
            val title =
                child
                    .children()
                    .firstOrNull { it.hasClass("section-title") }
                    ?.text()
                    ?.trim()
            listOf(StatementSection(kind, title, parseBlocks(child)))
        }
    }

    private fun Element.sectionKind(hasLegend: Boolean): SectionKind =
        when {
            hasClass("legend") -> SectionKind.Legend
            hasClass("input-specification") -> SectionKind.Input
            hasClass("output-specification") -> SectionKind.Output
            hasClass("interaction") || hasClass("interaction-specification") -> SectionKind.Interaction
            hasClass("scoring") -> SectionKind.Scoring
            hasClass("note") -> SectionKind.Note
            hasClass("sample-tests") -> SectionKind.Other
            children().any { it.hasClass("section-title") } -> SectionKind.Interaction
            !hasLegend -> SectionKind.Legend
            else -> SectionKind.Other
        }

    private fun parseBlocks(container: Element): List<StatementBlock> {
        val children = container.children().filterNot { it.hasClass("section-title") }
        return if (children.isNotEmpty()) {
            children.flatMap(::parseBlock)
        } else {
            parseParagraphBlocks(container)
        }
    }

    private fun parseBlock(element: Element): List<StatementBlock> =
        when (element.tagName()) {
            "p" -> parseParagraphBlocks(element)
            "pre" -> listOf(StatementBlock.Preformatted(rawText(element).trim()))
            "img" -> element.imageBlock()?.let(::listOf).orEmpty()
            "center" -> parseCenter(element)
            "table" -> listOf(parseTable(element))
            "ul", "ol" -> listOf(parseList(element))
            "div" -> parseBlocks(element)
            else -> parseParagraphBlocks(element)
        }

    private fun parseCenter(element: Element): List<StatementBlock> {
        val images = element.select("img").mapNotNull { it.imageBlock() }
        return if (images.isNotEmpty()) images else parseBlocks(element)
    }

    private fun Element.imageBlock(): StatementBlock.Image? {
        val url = absoluteUrl("src") ?: return null
        val alt = attr("alt").trim().takeIf { it.isNotEmpty() }
        val description =
            alt ?: parent()
                ?.selectFirst(".tex-font-size-small")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        return StatementBlock.Image(url, description)
    }

    private fun parseTable(element: Element): StatementBlock.Table {
        val rows =
            tableRows(element).map { row ->
                TableRow(
                    row.children().filter { it.tagName() == "th" || it.tagName() == "td" }.map { cell ->
                        TableCell(parseInlines(cell), cell.tagName() == "th")
                    },
                )
            }
        return StatementBlock.Table(rows)
    }

    private fun tableRows(element: Element): List<Element> =
        element.children().flatMap { child ->
            when (child.tagName()) {
                "tr" -> listOf(child)
                "thead", "tbody", "tfoot" -> tableRows(child)
                else -> emptyList()
            }
        }

    private fun parseList(element: Element): StatementBlock.ListBlock {
        val items = element.children().filter { it.tagName() == "li" }.map(::parseListItem)
        return StatementBlock.ListBlock(element.tagName() == "ol", items)
    }

    private fun parseListItem(item: Element): List<StatementBlock> {
        val parts = mutableListOf<InlinePart>()
        val blocks = mutableListOf<StatementBlock>()

        fun flush() {
            val inlines = cleanInlines(parts.filterIsInstance<InlinePart.Value>().map { it.inline })
            if (inlines.isNotEmpty()) blocks += StatementBlock.Paragraph(inlines)
            parts.clear()
        }

        item.childNodes().forEach { node ->
            val block = node as? Element
            if (block != null && block.tagName() in setOf("ul", "ol", "table", "pre", "p", "div")) {
                flush()
                blocks += parseBlock(block)
            } else {
                parts += parseInlineNode(node, InlineStyle())
            }
        }
        flush()
        return blocks
    }

    private fun parseParagraphBlocks(element: Element): List<StatementBlock> {
        val parts = parseInlineNodes(element, InlineStyle())
        val blocks = mutableListOf<StatementBlock>()
        val inlines = mutableListOf<Inline>()

        fun flush() {
            val cleaned = cleanInlines(inlines)
            if (cleaned.isNotEmpty()) blocks += StatementBlock.Paragraph(cleaned)
            inlines.clear()
        }

        parts.forEach { part ->
            when (part) {
                is InlinePart.Value -> inlines += part.inline
                is InlinePart.Display -> {
                    flush()
                    blocks += StatementBlock.DisplayMath(part.tex)
                }
            }
        }
        flush()
        return blocks
    }

    private fun parseInlines(element: Element): List<Inline> =
        cleanInlines(parseInlineNodes(element, InlineStyle()).filterIsInstance<InlinePart.Value>().map { it.inline })

    private fun parseInlineNodes(
        element: Element,
        style: InlineStyle,
    ): List<InlinePart> = element.childNodes().flatMap { parseInlineNode(it, style) }

    private fun parseInlineNode(
        node: Node,
        style: InlineStyle,
    ): List<InlinePart> =
        when (node) {
            is TextNode -> textParts(node.wholeText, style)
            is Element -> parseElementInline(node, style)
            else -> emptyList()
        }

    private fun parseElementInline(
        node: Element,
        style: InlineStyle,
    ): List<InlinePart> =
        when {
            node.hasClass("MathJax_Preview") -> emptyList()
            node.isTexScript() -> parseTexScript(node)
            node.tagName() == "br" -> listOf(InlinePart.Value(Inline.LineBreak))
            node.tagName() == "a" -> listOf(InlinePart.Value(node.toLink(style)))
            node.isBold() -> parseInlineNodes(node, style.copy(bold = true))
            node.isItalic() -> parseInlineNodes(node, style.copy(italic = true))
            node.isMonospace() -> parseInlineNodes(node, style.copy(monospace = true))
            node.tagName() == "img" -> emptyList()
            else -> parseInlineNodes(node, style)
        }

    private fun Element.isTexScript(): Boolean = tagName() == "script" && hasTexType()

    private fun Element.hasTexType(): Boolean = attr("type").lowercase(Locale.ROOT).contains("tex")

    private fun parseTexScript(node: Element): List<InlinePart> {
        val tex = Entities.unescape(node.data().trim()).takeIf { it.isNotEmpty() } ?: return emptyList()
        return if (node.previousElementSibling()?.hasClass("MathJax_Display") == true) {
            listOf(InlinePart.Display(tex))
        } else {
            listOf(InlinePart.Value(Inline.Math(tex)))
        }
    }

    private fun Element.toLink(style: InlineStyle): Inline.Link =
        Inline.Link(
            text = linkText(parseInlineNodes(this, style)),
            url = absoluteUrl("href").orEmpty(),
        )

    private fun Element.isBold(): Boolean = tagName() == "b" || tagName() == "strong" || hasClass("tex-font-style-bf")

    private fun Element.isItalic(): Boolean = tagName() == "i" || tagName() == "em" || hasClass("tex-font-style-it")

    private fun Element.isMonospace(): Boolean = tagName() == "tt" || tagName() == "code" || hasTexMonospaceClass()

    private fun Element.hasTexMonospaceClass(): Boolean = hasClass("tex-font-style-tt")

    private fun textParts(
        raw: String,
        style: InlineStyle,
    ): List<InlinePart> {
        val text = raw.replace(whitespace, " ")
        if (text.isBlank()) return emptyList()
        val parts = mutableListOf<InlinePart>()
        var end = 0
        math.findAll(text).forEach { match ->
            if (match.range.first > end) parts += textInline(text.substring(end, match.range.first), style)
            val displayTex = match.groupValues[1]
            val tex = Entities.unescape(displayTex.ifEmpty { match.groupValues[2] }.trim())
            if (displayTex.isNotEmpty()) {
                parts += InlinePart.Display(tex)
            } else {
                parts += InlinePart.Value(Inline.Math(tex))
            }
            end = match.range.last + 1
        }
        if (end < text.length) parts += textInline(text.substring(end), style)
        return parts
    }

    private fun textInline(
        text: String,
        style: InlineStyle,
    ): InlinePart = InlinePart.Value(Inline.Text(text, style.bold, style.italic, style.monospace))

    private fun linkText(parts: List<InlinePart>): String =
        parts
            .mapNotNull { part ->
                when (val inline = (part as? InlinePart.Value)?.inline) {
                    is Inline.Text -> inline.text
                    is Inline.Math -> inline.tex
                    is Inline.LineBreak -> "\n"
                    else -> null
                }
            }.joinToString("")
            .trim()

    @Suppress("ComplexCondition")
    private fun cleanInlines(inlines: List<Inline>): List<Inline> {
        val merged = mutableListOf<Inline>()
        inlines.forEach { inline ->
            val previous = merged.lastOrNull()
            if (previous is Inline.Text &&
                inline is Inline.Text &&
                previous.bold == inline.bold &&
                previous.italic == inline.italic &&
                previous.monospace == inline.monospace
            ) {
                merged[merged.lastIndex] = previous.copy(text = previous.text + inline.text)
            } else {
                merged += inline
            }
        }
        val firstText = merged.indexOfFirst { it is Inline.Text }
        if (firstText >= 0) {
            val first = merged[firstText] as Inline.Text
            merged[firstText] = first.copy(text = first.text.trimStart())
        }
        val lastText = merged.indexOfLast { it is Inline.Text }
        if (lastText >= 0) {
            val last = merged[lastText] as Inline.Text
            merged[lastText] = last.copy(text = last.text.trimEnd())
        }
        return merged.filterNot { it is Inline.Text && it.text.isEmpty() }
    }

    private fun parseSamples(root: Element): List<Sample> =
        root.select(".sample-test").map { sample ->
            Sample(
                input = sample.selectFirst(".input pre")?.sampleText().orEmpty(),
                output = sample.selectFirst(".output pre")?.sampleText().orEmpty(),
            )
        }

    private fun Element.sampleText(): String {
        val lines = select(".test-example-line")
        return if (lines.isNotEmpty()) {
            lines.joinToString("\n") { it.text().trim() }
        } else {
            rawText(this).trim()
        }
    }

    private data class InlineStyle(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val monospace: Boolean = false,
    )

    private sealed interface InlinePart {
        data class Value(
            val inline: Inline,
        ) : InlinePart

        data class Display(
            val tex: String,
        ) : InlinePart
    }
}
