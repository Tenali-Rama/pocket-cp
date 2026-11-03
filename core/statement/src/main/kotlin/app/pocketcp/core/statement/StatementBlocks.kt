@file:Suppress("TooManyFunctions")

package app.pocketcp.core.statement

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpTextButton
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

private const val MILLIS_PER_SECOND = 1_000L

internal fun LazyListScope.addSample(
    language: StatementLanguage,
    number: Int,
    sample: Sample,
    textScale: Float,
    modifier: Modifier,
) {
    item(key = "sample_$number", contentType = "sample") {
        SampleBlock(language, number, sample, textScale, modifier)
    }
}

internal fun LazyListScope.addMaterials(
    statement: Statement,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    if (statement.materials.isNotEmpty()) {
        item(key = "materials", contentType = "materials") {
            Materials(statement, textScale, onOpenLink, modifier)
        }
    }
}

@Composable
internal fun SampleBlock(
    language: StatementLanguage,
    number: Int,
    sample: Sample,
    textScale: Float,
    modifier: Modifier,
) {
    val context = contextForLanguage(LocalContext.current, language.key)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.sm)) {
        Text(
            text = context.getString(R.string.statement_example, number),
            style = statementStyle(PocketCpTheme.typography.title, textScale),
            color = PocketCpTheme.colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        SampleValue(
            language,
            number,
            sample.input,
            R.string.statement_input_label,
            R.string.statement_copy_input_description,
            textScale,
        )
        SampleValue(
            language,
            number,
            sample.output,
            R.string.statement_output_label,
            R.string.statement_copy_output_description,
            textScale,
        )
    }
}

@Composable
private fun SampleValue(
    language: StatementLanguage,
    number: Int,
    value: String,
    labelRes: Int,
    descriptionRes: Int,
    textScale: Float,
) {
    val context = contextForLanguage(LocalContext.current, language.key)
    val clipboard = LocalClipboardManager.current
    val description = context.getString(descriptionRes, number)
    Column(verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = context.getString(labelRes),
                style = statementStyle(PocketCpTheme.typography.label, textScale),
                color = PocketCpTheme.colors.onSurfaceMuted,
            )
            PcpTextButton(
                text = context.getString(R.string.statement_copy),
                onClick = { clipboard.setText(AnnotatedString(value)) },
                modifier =
                    Modifier
                        .sizeIn(minHeight = PocketCpTheme.spacing.touchTarget)
                        .testTag(
                            "sample_${number}_${if (labelRes == R.string.statement_input_label) "input" else "output"}_copy",
                        ).semantics {
                            contentDescription = description
                            role = Role.Button
                        },
            )
        }
        Preformatted(value, textScale, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun Materials(
    statement: Statement,
    textScale: Float,
    onOpenLink: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs)) {
        statement.materials.forEach { material ->
            Text(
                text = material.title,
                style =
                    statementStyle(PocketCpTheme.typography.body, textScale).copy(
                        color = PocketCpTheme.colors.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Button }
                        .clickable { onOpenLink(material.url) },
            )
        }
    }
}

@Composable
internal fun StatementImage(
    block: StatementBlock.Image,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val loader = LocalStatementImageLoader.current ?: remember(context) { ImageLoader(context) }
    SubcomposeAsyncImage(
        model =
            ImageRequest
                .Builder(context)
                .data(block.url)
                .crossfade(false)
                .build(),
        imageLoader = loader,
        contentDescription = block.description,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth(),
        loading = {
            Spacer(Modifier.fillMaxWidth().height(PocketCpTheme.spacing.imagePlaceholderHeight))
        },
        success = { state ->
            androidx.compose.foundation.Image(
                painter = state.painter,
                contentDescription = block.description,
                contentScale = ContentScale.Fit,
            )
        },
        error = {
            Text(
                text =
                    block.description ?: context.getString(R.string.statement_image_unavailable),
                style = PocketCpTheme.typography.bodySmall,
                color = PocketCpTheme.colors.onSurfaceMuted,
                modifier = Modifier.padding(vertical = PocketCpTheme.spacing.md),
            )
        },
    )
}

@Composable
internal fun Preformatted(
    text: String,
    textScale: Float,
    modifier: Modifier,
) {
    Text(
        text = text,
        style = statementStyle(PocketCpTheme.typography.code, textScale),
        color = PocketCpTheme.colors.onSurface,
        softWrap = false,
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(vertical = PocketCpTheme.spacing.xs),
    )
}

internal fun statementStyle(
    style: TextStyle,
    scale: Float,
): TextStyle =
    style.copy(
        fontSize = scaleTextUnit(style.fontSize, scale),
        lineHeight = scaleTextUnit(style.lineHeight, scale),
    )

private fun scaleTextUnit(
    value: TextUnit,
    scale: Float,
): TextUnit = if (value.isSp) value * scale else value

internal fun formatTimeLimit(milliseconds: Long): String =
    if (milliseconds % MILLIS_PER_SECOND == 0L) {
        "${milliseconds / MILLIS_PER_SECOND} s"
    } else {
        "$milliseconds ms"
    }

internal fun standardInput(
    context: Context,
    statement: Statement,
): String = contextFor(statement, context).getString(R.string.statement_standard_input)

internal fun standardOutput(
    context: Context,
    statement: Statement,
): String = contextFor(statement, context).getString(R.string.statement_standard_output)

internal fun sectionKind(
    context: Context,
    section: StatementSection,
    language: StatementLanguage,
): String = contextForLanguage(context, language.key).getString(section.kind.stringRes)

private val SectionKind.stringRes: Int
    get() =
        when (this) {
            SectionKind.Legend -> R.string.statement_section_legend
            SectionKind.Input -> R.string.statement_section_input
            SectionKind.Output -> R.string.statement_section_output
            SectionKind.Interaction -> R.string.statement_section_interaction
            SectionKind.Scoring -> R.string.statement_section_scoring
            SectionKind.Note -> R.string.statement_section_note
            SectionKind.Other -> R.string.statement_section_other
        }

internal fun localized(
    context: Context,
    statement: Statement,
    res: Int,
    vararg args: Any,
): String = contextFor(statement, context).resources.getString(res, *args)

private fun contextFor(
    statement: Statement,
    context: Context,
): Context = contextForLanguage(context, statement.language.key)

internal fun contextForLanguage(
    context: Context,
    language: String,
): Context {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(java.util.Locale.forLanguageTag(language))
    return context.createConfigurationContext(configuration)
}
