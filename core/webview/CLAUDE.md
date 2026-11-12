# core/webview

## Rules
- The only place the app creates a `WebView` (spec §2: login, submitting and the fallback browser only; never a
  main flow). Other modules embed `CodeforcesWebView`; they never build a `WebView` or a `WebViewClient` themselves.
- Every URL the WebView loads or follows passes `CodeforcesUrlPolicy` (ADR 0021). Widening the policy is a security
  decision: update the ADR and `CodeforcesUrlPolicyTest` in the same PR.
- File and content access stay off. JavaScript stays on only because Codeforces pages need MathJax.
- No navigation, strings or design tokens here: callers draw the chrome (notice, top bar, back) around the view.
- Tests: `CodeforcesUrlPolicyTest` is plain JUnit; the composable is tested on Robolectric (SDK 34) through
  `ShadowWebView` (loaded URL, client). Never load a live page.
