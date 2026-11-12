package app.pocketcp.core.statement

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.Inline
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Sample
import app.pocketcp.core.model.SectionKind
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementBlock
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.StatementSection
import app.pocketcp.core.model.TableCell
import app.pocketcp.core.model.TableRow
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private const val LARGE_TEXT_SCALE = 1.3f

private val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, content = content)
}

/** Gallery and variant screenshots for every renderer block, reading width and text-size preference. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [app.pocketcp.core.screenshot.SCREENSHOT_SDK])
class StatementScreenshotTest : ScreenshotTest() {
    @Test
    fun statementGallery() = captureMatrix("statementGallery", PocketCpThemeWrapper) { Gallery() }

    @Test
    fun narrowTablet() =
        captureSingle("statementGallery_narrow", ScreenshotConfig(Device.Tablet), PocketCpThemeWrapper) {
            Gallery(PocketCpTheme.spacing.readingWidthNarrow)
        }

    @Test
    fun fullTablet() =
        captureSingle("statementGallery_full", ScreenshotConfig(Device.Tablet), PocketCpThemeWrapper) {
            Gallery(Dp.Infinity)
        }

    @Test
    fun statementTextScale() =
        captureSingle("statementGallery_textScale130", ScreenshotConfig(), PocketCpThemeWrapper) {
            Gallery(textScale = LARGE_TEXT_SCALE)
        }

    @Test
    fun statementFontScale() =
        captureSingle("statementGallery_font150", ScreenshotConfig(fontScale = 1.5f), PocketCpThemeWrapper) { Gallery() }

    @Test
    fun imageLoading() =
        captureSingle("statementImage_loading", ScreenshotConfig(), PocketCpThemeWrapper) {
            ImageStateGallery("fake://loading")
        }

    @Test
    fun imageError() =
        captureSingle("statementImage_error", ScreenshotConfig(), PocketCpThemeWrapper) {
            ImageStateGallery("fake://error")
        }
}

@Composable
private fun Gallery(
    maxWidth: Dp = PocketCpTheme.spacing.readingWidthComfortable,
    textScale: Float = 1f,
) {
    FakeImageProvider {
        Box(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
            StatementContent(galleryStatement(), textScale, maxWidth, onOpenLink = {})
        }
    }
}

@Composable
private fun ImageStateGallery(url: String) {
    FakeImageProvider {
        Box(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
            StatementContent(imageStatement(url), textScale = 1f, maxWidth = Dp.Infinity, onOpenLink = {})
        }
    }
}

@Composable
private fun FakeImageProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember { fakeImageLoader(context) }
    CompositionLocalProvider(LocalStatementImageLoader provides imageLoader, content = content)
}

/**
 * Loads on the main thread, so Robolectric's idle wait covers the whole request. On Coil's default IO dispatcher the
 * capture raced the fetch and sometimes caught the loading placeholder (main run 41750884344).
 */
private fun fakeImageLoader(context: Context): ImageLoader =
    ImageLoader
        .Builder(context)
        .components { add(FakeFetcher.Factory()) }
        .dispatcher(Dispatchers.Main.immediate)
        .interceptorDispatcher(Dispatchers.Main.immediate)
        .build()

private class FakeFetcher(
    private val data: String,
) : Fetcher {
    override suspend fun fetch(): FetchResult? =
        when (data) {
            "fake://loading" -> awaitCancellation()
            "fake://error" -> null
            else -> DrawableResult(ColorDrawable(Color.DKGRAY), false, DataSource.MEMORY)
        }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? = if (data.scheme == "fake") FakeFetcher(data.toString()) else null
    }
}

private fun galleryStatement(): Statement =
    Statement(
        problem = ProblemId(1900, "A"),
        language = StatementLanguage.En,
        title = "A. Native Statement",
        timeLimitMillis = 2_000,
        memoryLimitMegabytes = 256,
        inputFile = null,
        outputFile = null,
        sections =
            listOf(
                StatementSection(
                    SectionKind.Legend,
                    null,
                    listOf(
                        StatementBlock.Paragraph(
                            listOf(
                                Inline.Text("Bold", bold = true),
                                Inline.Text(" and "),
                                Inline.Text("italic", italic = true),
                                Inline.Text(" plus "),
                                Inline.Text("code", monospace = true),
                                Inline.Text(" and "),
                                Inline.Link("a link", "https://codeforces.com"),
                                Inline.Text(" with "),
                                Inline.Math("x^2"),
                                Inline.LineBreak,
                                Inline.Text("The next line keeps paragraph flow."),
                            ),
                        ),
                        StatementBlock.DisplayMath("a^2 + b^2 = c^2"),
                        StatementBlock.ListBlock(
                            ordered = false,
                            items =
                                listOf(
                                    listOf(StatementBlock.Paragraph(listOf(Inline.Text("A bullet item")))),
                                    listOf(
                                        StatementBlock.Paragraph(listOf(Inline.Text("Nested list:"))),
                                        StatementBlock.ListBlock(
                                            ordered = true,
                                            items = listOf(listOf(StatementBlock.Paragraph(listOf(Inline.Text("First"))))),
                                        ),
                                    ),
                                ),
                        ),
                        StatementBlock.Table(
                            listOf(
                                TableRow(
                                    listOf(
                                        TableCell(listOf(Inline.Text("Very wide header", bold = true)), true),
                                        TableCell(listOf(Inline.Text("Value")), true),
                                    ),
                                ),
                                TableRow(
                                    listOf(TableCell(listOf(Inline.Text("Row one")), false), TableCell(listOf(Inline.Text("42")), false)),
                                ),
                            ),
                        ),
                        StatementBlock.Preformatted("for (i = 0; i < n; ++i) {\n    print(i);\n}"),
                        StatementBlock.Image("fake://image", "Sample diagram"),
                    ),
                ),
                StatementSection(
                    SectionKind.Input,
                    "Input",
                    listOf(StatementBlock.Paragraph(listOf(Inline.Text("The input contains n.")))),
                ),
                StatementSection(
                    SectionKind.Output,
                    "Output",
                    listOf(StatementBlock.Paragraph(listOf(Inline.Text("Print the answer.")))),
                ),
            ),
        samples = listOf(Sample("3\n1 2 3\n", "6\n")),
        materials = listOf(MaterialLink("Editorial", "https://codeforces.com/blog/1", MaterialKind.Tutorial)),
    )

private fun imageStatement(url: String): Statement =
    galleryStatement().copy(
        sections = listOf(StatementSection(SectionKind.Legend, "Image", listOf(StatementBlock.Image(url, null)))),
        samples = emptyList(),
    )
