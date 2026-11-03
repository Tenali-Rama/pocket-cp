@file:Suppress("TooManyFunctions")

package app.pocketcp.core.statement

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.model.TableCell
import app.pocketcp.core.model.TableRow
import app.pocketcp.core.statement.math.LocalMathRenderer

/**
 * Renders one problem statement as a lazy, native Compose reading view.
 *
 * [maxWidth] is normally a `PocketCpTheme.spacing` reading token. Pass [Dp.Infinity] to fill the pane. The caller
 * resolves settings into [textScale] and [maxWidth], keeping this module independent of `core/datastore`.
 */
@Composable
fun StatementContent(
    statement: Statement,
    textScale: Float,
    maxWidth: Dp,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: LazyListScope.() -> Unit = {},
) {
    val spacing = PocketCpTheme.spacing
    val contentModifier =
        if (maxWidth == Dp.Infinity) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = maxWidth).fillMaxWidth()
        }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = state,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        contentPadding = PaddingValues(spacing.lg),
    ) {
        header()
        item(key = "statement_header", contentType = "header") {
            StatementHeader(statement, textScale, contentModifier)
        }
        statement.sections.forEachIndexed { sectionIndex, section ->
            sectionItems(
                section,
                sectionIndex,
                SectionContent(statement.language, textScale, onOpenLink, contentModifier),
            )
        }
        statement.samples.forEachIndexed { sampleIndex, sample ->
            addSample(statement.language, sampleIndex + 1, sample, textScale, contentModifier)
        }
        addMaterials(statement, textScale, onOpenLink, contentModifier)
    }
}

private data class SectionContent(
    val language: StatementLanguage,
    val textScale: Float,
    val onOpenLink: (String) -> Unit,
    val modifier: Modifier,
)

private fun LazyListScope.sectionItems(
    section: StatementSection,
    sectionIndex: Int,
    content: SectionContent,
) {
    item(key = "section_${sectionIndex}_title", contentType = "section_title") {
        SectionTitle(content.language, section, content.textScale, content.modifier)
    }
    section.blocks.forEachIndexed { blockIndex, block ->
        item(key = "section_${sectionIndex}_block_$blockIndex", contentType = block::class) {
            StatementBlockContent(block, content.textScale, content.onOpenLink, content.modifier)
        }
    }
}

@Composable
private fun StatementHeader(
    statement: Statement,
    textScale: Float,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val typography = PocketCpTheme.typography
    val colors = PocketCpTheme.colors
    val titleStyle = statementStyle(typography.headline, textScale)
    val bodyStyle = statementStyle(typography.bodySmall, textScale)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs)) {
        Text(
            text = statement.title,
            style = titleStyle,
            color = colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        statement.timeLimitMillis?.let {
            Text(
                text = localized(context, statement, R.string.statement_time_limit, formatTimeLimit(it)),
                style = bodyStyle,
                color = colors.onSurfaceMuted,
            )
        }
        statement.memoryLimitMegabytes?.let {
            Text(
                text = localized(context, statement, R.string.statement_memory_limit, "$it MB"),
                style = bodyStyle,
                color = colors.onSurfaceMuted,
            )
        }
        Text(
            text =
                localized(
                    context,
                    statement,
                    R.string.statement_input,
                    statement.inputFile ?: standardInput(context, statement),
                ),
            style = bodyStyle,
            color = colors.onSurfaceMuted,
        )
        Text(
            text =
                localized(
                    context,
                    statement,
                    R.string.statement_output,
                    statement.outputFile ?: standardOutput(context, statement),
                ),
            style = bodyStyle,
            color = colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun SectionTitle(
    language: StatementLanguage,
    section: StatementSection,
    textScale: Float,
    modifier: Modifier,
) {
    val context = LocalContext.current
    Text(
        text = section.title?.takeIf { it.isNotBlank() } ?: sectionKind(context, section, language),
        style = statementStyle(PocketCpTheme.typography.title, textScale),
        color = PocketCpTheme.colors.onBackground,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun StatementBlockContent(
    block: StatementBlock,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    when (block) {
        is StatementBlock.Paragraph -> InlineText(block.inlines, textScale, onOpenLink, modifier)
        is StatementBlock.DisplayMath ->
            Box(modifier) {
                LocalMathRenderer.current.Display(block.tex, textScale)
            }
        is StatementBlock.Image -> StatementImage(block, textScale, modifier)
        is StatementBlock.Table -> StatementTable(block.rows, textScale, onOpenLink, modifier)
        is StatementBlock.ListBlock -> StatementList(block, textScale, onOpenLink, modifier)
        is StatementBlock.Preformatted -> Preformatted(block.text, textScale, modifier)
    }
}

@Composable
private fun InlineText(
    inlines: List<Inline>,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
    style: TextStyle = statementStyle(PocketCpTheme.typography.body, textScale),
) {
    val annotated = annotatedText(inlines, onOpenLink)
    val inlineContent =
        inlines
            .mapIndexedNotNull { index, inline ->
                if (inline is Inline.Math) {
                    "math_$index" to
                        InlineTextContent(
                            placeholder =
                                androidx.compose.ui.text.Placeholder(
                                    width = style.fontSize * 2f,
                                    height = style.lineHeight,
                                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center,
                                ),
                        ) {
                            LocalMathRenderer.current.Inline(inline.tex, textScale)
                        }
                } else {
                    null
                }
            }.toMap()
    BasicText(
        text = annotated,
        style = style.copy(color = PocketCpTheme.colors.onSurface),
        inlineContent = inlineContent,
        modifier = modifier.padding(vertical = PocketCpTheme.spacing.xs),
    )
}

@Composable
private fun annotatedText(
    inlines: List<Inline>,
    onOpenLink: (String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        inlines.forEachIndexed { index, inline ->
            when (inline) {
                is Inline.Text -> {
                    val weight = if (inline.bold) FontWeight.Bold else null
                    val style =
                        if (inline.italic) {
                            androidx.compose.ui.text.font.FontStyle.Italic
                        } else {
                            null
                        }
                    withStyle(
                        SpanStyle(
                            fontWeight = weight,
                            fontStyle = style,
                            fontFamily = if (inline.monospace) PocketCpTheme.typography.code.fontFamily else null,
                        ),
                    ) { append(inline.text) }
                }
                is Inline.Math -> appendInlineContent("math_$index", inline.tex)
                is Inline.Link -> {
                    withLink(
                        LinkAnnotation.Url(
                            url = inline.url,
                            styles =
                                TextLinkStyles(
                                    style =
                                        SpanStyle(
                                            color = PocketCpTheme.colors.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                ),
                            linkInteractionListener = { link ->
                                onOpenLink((link as LinkAnnotation.Url).url)
                            },
                        ),
                    ) { append(inline.text) }
                }
                Inline.LineBreak -> append('\n')
            }
        }
    }

@Composable
private fun StatementList(
    block: StatementBlock.ListBlock,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    val spacing = PocketCpTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        block.items.forEachIndexed { index, itemBlocks ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    style = statementStyle(PocketCpTheme.typography.body, textScale),
                    color = PocketCpTheme.colors.onSurface,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    itemBlocks.forEach { child ->
                        StatementBlockContent(child, textScale, onOpenLink, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementTable(
    rows: List<TableRow>,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.horizontalScroll(scrollState).width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xxs),
    ) {
        rows.forEach { row ->
            TableRowContent(row, textScale, onOpenLink)
        }
    }
}

@Composable
private fun TableRowContent(
    row: TableRow,
    textScale: Float,
    onOpenLink: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        row.cells.forEach { cell ->
            TableCellContent(cell, textScale, onOpenLink)
        }
    }
}

@Composable
private fun TableCellContent(
    cell: TableCell,
    textScale: Float,
    onOpenLink: (String) -> Unit,
) {
    val spacing = PocketCpTheme.spacing
    val style = if (cell.header) PocketCpTheme.typography.title else PocketCpTheme.typography.body
    InlineText(
        inlines = cell.inlines,
        textScale = textScale,
        onOpenLink = onOpenLink,
        modifier = Modifier.padding(horizontal = spacing.md).width(IntrinsicSize.Max),
        style = statementStyle(style, textScale),
    )
}
