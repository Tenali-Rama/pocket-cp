---
id: 044
title: Native statement renderer in core/statement (text, lists, tables, samples with copy, images)
milestone: M3
status: todo
deps: [039]
touches: [core/statement/, settings.gradle.kts+, core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/Dimensions.kt, core/designsystem/src/test/kotlin/app/pocketcp/core/designsystem/ThemeTokens.kt, core/designsystem/src/test/screenshots/, core/designsystem/API.md, gradle/libs.versions.toml, config/perf-budgets.json, docs/decisions/]
spec: ["7.4 Problem View", "2. Product Principles", "5. Design & Layout", "3. Performance Budgets"]
---

## Goal
Spec §2 wants native screens, not a wrapper. After this task, `core/statement` renders a `Statement` (039) with Compose: header, sections, styled text, lists, tables, preformatted text, images, and samples with a Copy button. It respects the reading text size and width. Math goes through a `MathRenderer` slot: this task's default shows the TeX source, and 045 swaps in native typesetting.

## Acceptance criteria
- [ ] New module `core/statement` (`pocketcp.android.library` + compose), with `CLAUDE.md` and `API.md`. Its dependencies are exactly `api(project(":core:model"))`, `implementation(project(":core:designsystem"))`, `implementation(project(":core:network"))` (the shared `OkHttpClient` for images) and Coil. It does **not** depend on `core/datastore`: callers resolve preferences to plain values.
  - `StatementContent(statement, textScale: Float, maxWidth: Dp, onOpenLink: (String) -> Unit, modifier, state: LazyListState = rememberLazyListState(), header: LazyListScope.() -> Unit = {})` is one `LazyColumn` with an item per block, so long statements scroll lazily; `state` lets the caller scroll it (046's PageUp/PageDown)
  - its column is centred and capped at `maxWidth`; `Dp.Infinity` fills the pane. 046 maps `StatementWidthPreference` to the tokens below, and `statementTextSize` to `textScale`
  - `textScale` multiplies the statement's text styles on top of the app's text size (and the system font scale)
  - only design-system tokens: no hard-coded colours or sizes
- [ ] Design-system tokens, in `Dimensions.kt` only (core/designsystem rule): `PocketCpSpacing` gains `readingWidthNarrow` (560 dp), `readingWidthComfortable` (720 dp) and `imagePlaceholderHeight` (160 dp) (default — owner may change), the same at both densities. `ThemeTokens.kt` shows them, the `themeTokens_*` goldens are re-accepted, and `core/designsystem/API.md` lists them.
- [ ] Rendering:
  - header: title, then "time limit per test", "memory limit per test" and input/output ("standard input"/"standard output" when `null`), in the statement's language (`en` and `ru` strings)
  - each section's title as a heading (a11y `heading()`)
  - paragraphs with bold, italic, monospace and links (links call `onOpenLink`)
  - ordered and bulleted lists, nested
  - tables scroll horizontally when wider than the column; header cells use the heading style
  - `Preformatted` text in the code font, scrolling horizontally
- [ ] Samples: "Example N" with Input and Output in the code font, scrolling horizontally and never wrapping. Each block has a Copy button that puts exactly that text on the clipboard. The buttons are tagged `sample_<n>_input_copy` / `sample_<n>_output_copy`, have a content description ("Copy input of example 1") and are ≥ `spacing.touchTarget`.
- [ ] Math slot: `LocalMathRenderer` with `Inline` and `Display` composables, in `core/statement/src/main/kotlin/app/pocketcp/core/statement/math/MathRenderer.kt` so that 045 only changes the `math/` package. The default shows the TeX source in the code font with a `math` content description; `Inline.Math` sits in the text flow via inline content.
- [ ] Images: Coil, with an `ImageLoader` whose network fetcher uses `core/network`'s single `OkHttpClient` (shared cookie jar, HTTPS only) and a disk cache, so images of viewed statements show offline. While loading, a placeholder of `spacing.imagePlaceholderHeight`; on failure, the `description`, or "Image unavailable". A new ADR: images use the shared client but not the `RequestQueue` (binary bodies, no API spacing) (default — owner may change).
- [ ] Tests: `StatementContentTest` (Compose, Robolectric):
  - Copy puts the exact sample text (with newlines) on the clipboard
  - a link tap calls `onOpenLink` with the URL
  - the section headings are headings
  - the copy buttons meet the touch-target size

  Screenshots use a hand-built `galleryStatement()` with every block type and a fake image loader:
  - `statementGallery_<device>_<theme>`: on tablet and desktop the column is centred at `readingWidthComfortable`
  - `statementGallery_narrow_tablet_dark` (`readingWidthNarrow`) and `statementGallery_full_tablet_dark` (`Dp.Infinity`)
  - `statementGallery_textScale130_phone_dark` (`textScale = 1.3f`, the `Largest` factor)
  - `statementGallery_font150_phone_dark`
  - `statementImage_loading_phone_dark` and `statementImage_error_phone_dark`
- [ ] If the release APK goes over its `perf` budget, the PR raises `release_apk_bytes` under ADR 0013, with the reason and the artifact values, and rebased on whatever the in-flight `chore/apk-budget-margin` landed.

## Out of scope
- Typesetting TeX (045). "Send to editor" on samples: the editor is M5 (default — owner may change).
- The problem screen, its top bar and states (046). WebView anything (047).

## Notes for next
<!-- builder fills in -->
