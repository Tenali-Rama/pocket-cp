package app.pocketcp.core.webview

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A WebView restricted to Codeforces ([CodeforcesUrlPolicy]): it loads [url] only when allowed, and any navigation
 * of the page to another URL is cancelled and handed to [onExternalLink] (the caller opens it elsewhere or drops it).
 * A disallowed [url] itself loads nothing and goes to [onExternalLink] too.
 * No file or content access; JavaScript is on, because Codeforces pages typeset their math with MathJax.
 */
@Composable
fun CodeforcesWebView(
    url: String,
    onExternalLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnExternalLink by rememberUpdatedState(onExternalLink)
    // A new URL is a new page: a fresh WebView, so the old page's history never carries over.
    key(url) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configure(CodeforcesWebViewClient { currentOnExternalLink(it) })
                    if (CodeforcesUrlPolicy.isAllowed(url)) loadUrl(url) else currentOnExternalLink(url)
                }
            },
            modifier = modifier,
            onRelease = WebView::destroy,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configure(client: WebViewClient) {
    settings.javaScriptEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    webViewClient = client
}

/**
 * Lets the WebView follow only allowed URLs. A disallowed navigation of the page itself goes to [onExternalLink];
 * one inside a frame (an embed or an ad) is only cancelled, so a frame never opens anything by itself.
 */
internal class CodeforcesWebViewClient(
    private val onExternalLink: (String) -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = intercept(request.url.toString(), request.isForMainFrame)

    /** Whether to cancel the navigation to [url]; `false` lets the WebView load it. */
    fun intercept(
        url: String,
        mainFrame: Boolean,
    ): Boolean {
        if (CodeforcesUrlPolicy.isAllowed(url)) return false
        if (mainFrame) onExternalLink(url)
        return true
    }
}
