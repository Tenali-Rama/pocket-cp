# core/webview API

The app's only WebView, restricted to Codeforces domains (spec §16, ADR 0021): `CodeforcesUrlPolicy` and the `CodeforcesWebView` composable.

Package `app.pocketcp.core.webview`. Add with `implementation(project(":core:webview"))`.

```kotlin
CodeforcesWebView(
    url = statements.pageUrl(problem, language),
    onExternalLink = actions::openLink, // anything not on Codeforces: system browser, or drop it
    modifier = Modifier.fillMaxSize(),
)
```

| Symbol | What it is |
|--------|-----------|
| `CodeforcesUrlPolicy.isAllowed(url)` | `true` only for `https` URLs whose host is `codeforces.com` or ends in `.codeforces.com`. Everything else (`http`, look-alike hosts, `javascript:`, `file:`, `content:`, `intent:`, non-URLs) is `false`. |
| `CodeforcesWebView(url, onExternalLink, modifier)` | Loads `url` when allowed; a disallowed `url` loads nothing and goes to `onExternalLink`. The page navigating to a disallowed URL is cancelled and handed to `onExternalLink`; inside a frame it is only cancelled. JavaScript on (MathJax), no file or content access. A new `url` gets a fresh WebView; the WebView is destroyed when it leaves the composition. |

On the JVM (Robolectric) the WebView draws nothing: screenshots show the chrome around it, and tests read the
loaded URL with `shadowOf(webView).lastLoadedUrl`.
