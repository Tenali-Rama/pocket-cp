package app.pocketcp.core.webview

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CodeforcesWebViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val external = mutableListOf<String>()

    @Test
    fun allowedUrl_loads_withJavaScript_andNoFileOrContentAccess() {
        val webView = show("https://codeforces.com/contest/2274/problem/A")

        assertEquals("https://codeforces.com/contest/2274/problem/A", shadowOf(webView).lastLoadedUrl)
        assertTrue(webView.settings.javaScriptEnabled)
        assertFalse(webView.settings.allowFileAccess)
        assertFalse(webView.settings.allowContentAccess)
        assertEquals(emptyList<String>(), external)
    }

    @Test
    fun disallowedUrl_loadsNothing_andGoesToOnExternalLink() {
        val webView = show("https://evilcodeforces.com/")

        assertNull(shadowOf(webView).lastLoadedUrl)
        assertEquals(listOf("https://evilcodeforces.com/"), external)
    }

    @Test
    fun navigationOffCodeforces_isCancelled_andHandedToOnExternalLink() {
        val webView = show("https://codeforces.com/blog/entry/1")
        val client = shadowOf(webView).webViewClient

        val cancelled = client.shouldOverrideUrlLoading(webView, Request("https://github.com/x", mainFrame = true))

        assertTrue(cancelled)
        assertEquals(listOf("https://github.com/x"), external)
        assertEquals("https://codeforces.com/blog/entry/1", shadowOf(webView).lastLoadedUrl)
    }

    @Test
    fun navigationWithinCodeforces_isLetThrough() {
        val webView = show("https://codeforces.com/blog/entry/1")
        val client = shadowOf(webView).webViewClient

        val request = Request("https://m1.codeforces.com/contest/1", mainFrame = true)
        assertFalse(client.shouldOverrideUrlLoading(webView, request))
        assertEquals(emptyList<String>(), external)
    }

    @Test
    fun disallowedFrameNavigation_isCancelled_withoutOpeningAnything() {
        val webView = show("https://codeforces.com/blog/entry/1")
        val client = shadowOf(webView).webViewClient

        assertTrue(client.shouldOverrideUrlLoading(webView, Request("http://ads.example/x", mainFrame = false)))
        assertEquals(emptyList<String>(), external)
    }

    private fun show(url: String): WebView {
        rule.setContent { CodeforcesWebView(url, onExternalLink = { external += it }) }
        rule.waitForIdle()
        return checkNotNull(shownWebView()) { "No WebView in the hierarchy" }
    }

    private class Request(
        private val target: String,
        private val mainFrame: Boolean,
    ) : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(target)

        override fun isForMainFrame(): Boolean = mainFrame

        override fun isRedirect(): Boolean = false

        override fun hasGesture(): Boolean = true

        override fun getMethod(): String = "GET"

        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    private fun shownWebView(): WebView? {
        val root: View = rule.activity.window.decorView
        return root.findWebView()
    }
}

private fun View.findWebView(): WebView? =
    when (this) {
        is WebView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findWebView() }
        else -> null
    }
