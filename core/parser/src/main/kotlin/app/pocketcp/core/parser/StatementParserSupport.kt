@file:Suppress("ReturnCount", "MagicNumber")

package app.pocketcp.core.parser

import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.Locale

internal fun parseMaterials(document: Document): List<MaterialLink> =
    document
        .select(".contest-materials, .contest-materials-list, .roundbox.sidebox")
        .flatMap { box ->
            if (box.hasClass("sidebox") &&
                !box
                    .selectFirst(".caption")
                    ?.text()
                    .orEmpty()
                    .isMaterialHeading()
            ) {
                emptyList()
            } else {
                box.select("a")
            }
        }.mapNotNull { link ->
            val url = link.absoluteUrl("href") ?: return@mapNotNull null
            val title = link.text().trim()
            if (title.isEmpty()) return@mapNotNull null
            MaterialLink(title, url, materialKind(title))
        }

private fun materialKind(title: String): MaterialKind {
    val lower = title.lowercase(Locale.ROOT)
    return when {
        "tutorial" in lower || "editorial" in lower || "разбор" in lower -> MaterialKind.Tutorial
        "announcement" in lower || "анонс" in lower -> MaterialKind.Announcement
        else -> MaterialKind.Other
    }
}

internal fun Element.fileName(selector: String): String? {
    val file = selectFirst(selector) ?: return null
    if (file.hasClass("input-standard") || file.hasClass("output-standard")) return null
    return file.ownText().trim().takeIf { it.isNotEmpty() }
        ?: file
            .children()
            .filterNot { it.hasClass("property-title") }
            .joinToString(" ") { it.text() }
            .trim()
            .takeIf { it.isNotEmpty() }
}

private fun String.isMaterialHeading(): Boolean {
    val lower = lowercase(Locale.ROOT)
    return "contest materials" in lower || "материалы конкурса" in lower
}

internal fun String.removeProblemPrefix(): String = replaceFirst(Regex("^[A-Za-z][0-9]*\\.\\s*"), "")

private val number = Regex("(\\d+(?:\\.\\d+)?)")

internal fun String.toMillis(): Long? {
    val value =
        number
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull() ?: return null
    return if (lowercase(Locale.ROOT).contains("ms") || lowercase(Locale.ROOT).contains("миллисек")) {
        value.toLong()
    } else {
        (value * 1_000).toLong()
    }
}

internal fun String.toMegabytes(): Int? {
    val value =
        number
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull() ?: return null
    val lower = lowercase(Locale.ROOT)
    return when {
        "kb" in lower || "килобайт" in lower -> (value / 1_024).toInt()
        else -> value.toInt()
    }
}

private const val BASE_URL = "https://codeforces.com"

internal fun Element.absoluteUrl(attribute: String): String? {
    val raw = attr(attribute).trim()
    if (raw.isEmpty()) return null
    val resolved = absUrl(attribute).takeIf { it.isNotEmpty() }
    return resolved ?: when {
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("/") -> "$BASE_URL$raw"
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        else -> "$BASE_URL/${raw.trimStart('/')}"
    }
}

internal fun rawText(node: Node): String =
    when (node) {
        is TextNode -> node.wholeText
        is Element -> if (node.tagName() == "br") "\n" else node.childNodes().joinToString("") { rawText(it) }
        else -> ""
    }
