package app.pocketcp.core.problemview

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
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.statement.LocalStatementImageLoader
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers

internal val PocketCpThemeWrapper: ThemeWrapper = { dark, content ->
    PocketCpTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, content = content)
}

/** [content] on the theme's background, with statement images answered in memory (no network in tests). */
@Composable
internal fun ScreenshotFrame(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember { fakeImageLoader(context) }
    CompositionLocalProvider(LocalStatementImageLoader provides imageLoader) {
        Box(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) { content() }
    }
}

/** Loads on the main thread, so Robolectric's idle wait covers each request (as in core/statement's tests). */
private fun fakeImageLoader(context: Context): ImageLoader =
    ImageLoader
        .Builder(context)
        .components { add(GreyImageFetcher.Factory()) }
        .dispatcher(Dispatchers.Main.immediate)
        .interceptorDispatcher(Dispatchers.Main.immediate)
        .build()

/** Every statement image is a grey block. */
private class GreyImageFetcher : Fetcher {
    override suspend fun fetch(): FetchResult = DrawableResult(ColorDrawable(Color.DKGRAY), false, DataSource.MEMORY)

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = GreyImageFetcher()
    }
}
