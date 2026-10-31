---
id: 047
title: Codeforces-only WebView as the per-page statement fallback, and contest materials links
milestone: M3
status: todo
deps: [046]
touches: [core/webview/, settings.gradle.kts+, core/problemview/src/main/, core/problemview/src/test/, core/problemview/build.gradle.kts, core/problemview/API.md, core/problemview/CLAUDE.md, gradle/libs.versions.toml, docs/decisions/]
spec: ["18. Known Risks", "16. Non-Functional", "7.4 Problem View", "2. Product Principles"]
---

## Goal
Spec §18: when the parser can't read a page, that page falls back to the WebView. After this task, an unparsable statement shows the Codeforces page in a WebView inside the problem view, restricted to Codeforces domains (spec §16). The problem view also lists the contest's materials (announcement, tutorial) and opens them, and in-statement Codeforces links, in the same restricted WebView. Native editorial reading stays M9.

## Acceptance criteria
- [ ] New module `core/webview`, with `CLAUDE.md` and `API.md`:
  - `CodeforcesUrlPolicy.isAllowed(url)`: `https` only, host `codeforces.com` or a subdomain of it
  - `CodeforcesWebView(url, onExternalLink: (String) -> Unit, modifier)`:
    - it loads only allowed URLs
    - navigating to any other URL is cancelled and handed to `onExternalLink`
    - no file or content access
    - JavaScript on, because pages need MathJax (default — owner may change)

  A new ADR records the domain policy that the M4 login and fallback browser will reuse.
- [ ] `CodeforcesUrlPolicyTest` covers:
  - allowed: `https://codeforces.com/…`, `https://m1.codeforces.com/…`, `https://espresso.codeforces.com/…`
  - rejected: `http://codeforces.com`, `https://codeforces.com.evil.io`, `https://evilcodeforces.com`, `javascript:`, `file:`, `intent:`
- [ ] Problem view (`core/problemview`):
  - `Unparsable` shows `CodeforcesWebView` of `StatementRepository.pageUrl(problem, language)` under a one-line notice ("Shown from Codeforces"), tagged `problem_fallback`. `Offline` and the other errors keep 046's `ErrorState`.
  - When `statement.materials` isn't empty, a "Contest materials" action (`problem_materials`) lists them (`problem_material_<n>`). Tapping one opens it in the WebView as a page over the statement, with a back arrow and `BackHandler`.
  - `openLink(url)` opens allowed URLs in that WebView page, and other URLs in the system browser (default — owner may change).
- [ ] Tests (Robolectric):
  - the fallback state's WebView loaded the page URL (`ShadowWebView`)
  - a disallowed navigation calls `onExternalLink` and loads nothing
  - a material tap opens the page, and back closes it
  - `ProblemViewModelTest` gains the link routing
- [ ] Screenshots (WebView content is blank on the JVM; these show the chrome around it):
  - `problemView_fallback_phone_dark`
  - `problemView_materials_<device>_<theme>` (the materials list open)
  - `problemView_materialPage_tablet_dark`

## Out of scope
- Sharing the OkHttp cookie jar with the WebView, login, and the general fallback browser with deep links (M4).
- A Maestro step for the fallback: it would load live Codeforces in CI.

## Notes for next
<!-- builder fills in -->
