---
id: 044
title: Native statement renderer in core/statement (text, lists, tables, samples with copy, images)
milestone: M3
status: todo
deps: [039]
touches: [core/statement/, settings.gradle.kts+, gradle/libs.versions.toml, config/perf-budgets.json, docs/decisions/]
spec: ["7.4 Problem View", "2. Product Principles", "5. Design & Layout", "3. Performance Budgets"]
---

## Goal
Spec §2 wants native screens, not a wrapper. After this task, `core/statement` renders a `Statement` (039) with Compose: header, sections, styled text, lists, tables, preformatted text, images, and samples with a Copy button. It respects the reading text size and width. Math goes through a `MathRenderer` slot: this task's default shows the TeX source, and 045 swaps in native typesetting.

## Acceptance criteria
- [ ] New module `core/statement` (`pocketcp.android.library` + compose, `api(project(":core:model"))`), with `CLAUDE.md` and `API.md`:
  - `StatementContent(statement, textSize: TextSizePreference, width: StatementWidthPreference, onOpenLink: (String) -> Unit, modifier, header: LazyListScope.() -> Unit = {})` is one `LazyColumn` with an item per block, so long statements scroll lazily
  - its column is centred and capped by the width (`Narrow`, `Comfortable` and `Full` map to design-system dimension tokens; `Full` fills the pane)
  - `textSize` scales the statement text on top of the app's text size
  - only design-system tokens: no hard-coded colours or sizes
- [ ] Rendering:
  - header: title, then "time limit per test", "memory limit per test" and input/output ("standard input"/"standard output" when `null`), in the statement's language (`en` and `ru` strings)
  - each section's title as a heading (a11y `heading()`)
  - paragraphs with bold, italic, monospace and links (links call `onOpenLink`)
  - ordered and bulleted lists, nested
  - tables scroll horizontally when wider than the column; header cells use the heading style
  - `Preformatted` text in the code font, scrolling horizontally
- [ ] Samples: "Example N" with Input and Output in the code font, scrolling horizontally and never wrapping. Each block has a Copy button that puts exactly that text on the clipboard. The buttons are tagged `sample_<n>_input_copy` / `sample_<n>_output_copy`, have a content description ("Copy input of example 1") and are ≥ `spacing.touchTarget`.
- [ ] Math slot: `LocalMathRenderer` with `Inline` and `Display` composables, in `core/statement/src/main/kotlin/app/pocketcp/core/statement/math/MathRenderer.kt` so that 045 only changes the `math/` package. The default shows the TeX source in the code font with a `math` content description; `Inline.Math` sits in the text flow via inline content.
- [ ] Images: Coil, with an `ImageLoader` whose network fetcher uses `core/network`'s single `OkHttpClient` (shared cookie jar, HTTPS only) and a disk cache, so images of viewed statements show offline. While loading, a placeholder of token size; on failure, the `description`, or "Image unavailable". A new ADR: images use the shared client but not the `RequestQueue` (binary bodies, no API spacing) (default — owner may change).
- [ ] Tests: `StatementContentTest` (Compose, Robolectric):
  - Copy puts the exact sample text (with newlines) on the clipboard
  - a link tap calls `onOpenLink` with the URL
  - the section headings are headings
  - the copy buttons meet the touch-target size

  Screenshots use a hand-built `galleryStatement()` with every block type and a fake image loader:
  - `statementGallery_<device>_<theme>`: on tablet and desktop the column is centred at the `Comfortable` width
  - `statementGallery_narrow_tablet_dark` and `statementGallery_full_tablet_dark`
  - `statementGallery_textLargest_phone_dark`
  - `statementGallery_font150_phone_dark`
  - `statementImage_loading_phone_dark` and `statementImage_error_phone_dark`
- [ ] If the release APK goes over its `perf` budget, the PR raises `release_apk_bytes` under ADR 0013, with the reason and the artifact values, and rebased on whatever the in-flight `chore/apk-budget-margin` landed.

## Out of scope
- Typesetting TeX (045). "Send to editor" on samples: the editor is M5 (default — owner may change).
- The problem screen, its top bar and states (046). WebView anything (047).

## Notes for next
<!-- builder fills in -->
