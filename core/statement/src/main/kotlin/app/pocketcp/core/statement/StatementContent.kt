package app.pocketcp.core.statement

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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

private const val LINK_TAG = "statement_link"

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
            Modifier.widthIn(max = maxWidth)
        }
    LazyColumn(
        modifier = modifier,
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
            sectionItems(statement.language, section, sectionIndex, textScale, onOpenLink, contentModifier)
        }
        statement.samples.forEachIndexed { sampleIndex, sample ->
            addSample(statement.language, sampleIndex + 1, sample, textScale, contentModifier)
        }
        addMaterials(statement, textScale, onOpenLink, contentModifier)
    }
}

private fun LazyListScope.sectionItems(
    language: StatementLanguage,
    section: StatementSection,
    sectionIndex: Int,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    item(key = "section_${sectionIndex}_title", contentType = "section_title") {
        SectionTitle(language, section, textScale, modifier)
    }
    section.blocks.forEachIndexed { blockIndex, block ->
        item(key = "section_${sectionIndex}_block_$blockIndex", contentType = block::class) {
            StatementBlockContent(block, textScale, onOpenLink, modifier)
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
            text = localized(context, statement, R.string.statement_input, statement.inputFile ?: standardInput(context, statement)),
            style = bodyStyle,
            color = colors.onSurfaceMuted,
        )
        Text(
            text = localized(context, statement, R.string.statement_output, statement.outputFile ?: standardOutput(context, statement)),
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
                LocalMathRenderer.current.Display(block.tex)
            }
        is StatementBlock.Image -> StatementImage(block, modifier)
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
    val annotated = annotatedText(inlines)
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
                            LocalMathRenderer.current.Inline(inline.tex)
                        }
                } else {
                    null
                }
            }.toMap()
    val links = annotated.getStringAnnotations(LINK_TAG, 0, annotated.length)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textModifier =
        modifier
            .padding(vertical = PocketCpTheme.spacing.xs)
            .pointerInput(annotated) {
                detectTapGestures { position ->
                    layoutResult?.getOffsetForPosition(position)?.let { offset ->
                        annotated.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()?.let { annotation ->
                            onOpenLink(annotation.item)
                        }
                    }
                }
            }.semantics {
                if (links.size == 1) {
                    onClick {
                        onOpenLink(links.first().item)
                        true
                    }
                }
            }
    BasicText(
        text = annotated,
        style = style.copy(color = PocketCpTheme.colors.onSurface),
        inlineContent = inlineContent,
        modifier = textModifier,
        onTextLayout = { layoutResult = it },
    )
}

@Composable
private fun annotatedText(inlines: List<Inline>): AnnotatedString =
    buildAnnotatedString {
        inlines.forEachIndexed { index, inline ->
            when (inline) {
                is Inline.Text -> {
                    val weight = if (inline.bold) FontWeight.Bold else FontWeight.Normal
                    val style =
                        if (inline.italic) {
                            androidx.compose.ui.text.font.FontStyle.Italic
                        } else {
                            androidx.compose.ui.text.font.FontStyle.Normal
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
                    pushStringAnnotation(LINK_TAG, inline.url)
                    withStyle(
                        SpanStyle(
                            color = PocketCpTheme.colors.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) { append(inline.text) }
                    pop()
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
