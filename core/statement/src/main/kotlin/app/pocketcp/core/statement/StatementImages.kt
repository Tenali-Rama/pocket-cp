package app.pocketcp.core.statement

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader
import coil.disk.DiskCache
import okhttp3.OkHttpClient
import java.io.File

private const val IMAGE_CACHE_DIRECTORY = "statement-images"
private const val IMAGE_CACHE_PERCENT = 0.02

/**
 * Builds the image loader for statements. The supplied client must be the singleton from `core/network`; that keeps
 * cookies and HTTPS-only transport shared while this loader's disk cache keeps viewed images available offline.
 */
fun statementImageLoader(
    context: Context,
    client: OkHttpClient,
): ImageLoader =
    ImageLoader
        .Builder(context)
        .okHttpClient(client)
        .diskCache {
            DiskCache
                .Builder()
                .directory(File(context.cacheDir, IMAGE_CACHE_DIRECTORY))
                .maxSizePercent(IMAGE_CACHE_PERCENT)
                .build()
        }.build()

/** Supplies a shared-client image loader to [StatementContent], useful at the app composition root and in tests. */
@Composable
fun ProvideStatementImageLoader(
    client: OkHttpClient,
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loader = androidx.compose.runtime.remember(context, client) { statementImageLoader(context, client) }
    CompositionLocalProvider(LocalStatementImageLoader provides loader, content = content)
}

/** The image loader selected by the statement composition. */
val LocalStatementImageLoader = staticCompositionLocalOf<ImageLoader?> { null }
