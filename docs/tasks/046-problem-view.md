---
id: 046
title: Problem view in core/problemview with EN/RU, text size, reading width and cache-first states
milestone: M3
status: todo
deps: [043, 044]
touches: [core/problemview/, settings.gradle.kts+, gradle/libs.versions.toml, docs/decisions/0012-feature-view-models.md]
spec: ["7.4 Problem View", "3. Performance Budgets", "14. Offline", "5. Design & Layout"]
---

## Goal
After this task there is one problem view that any feature can embed, as a page or as the detail pane. It shows a problem's statement from the cache at once, refreshes it in the background, and lets the user switch English/Russian and change text size and reading width, all persisted. Problemset (048) and Contests (049) embed it. Features never depend on each other, so it lives in `core/`.

## Acceptance criteria
- [ ] New module `core/problemview` (android library + compose), with `CLAUDE.md` and `API.md`. It depends on `core/data`, `core/datastore`, `core/statement`, `core/ui` and `core/designsystem`.
  - `ProblemViewModelFactory @Inject constructor(...)` with `create(problem: ProblemId): ProblemViewModel`. Features inject the factory and build the view model with `viewModel(key = problem.display) { factory.create(problem) }`.
  - So adding a dependency to the view model never edits a feature. ADR 0012 gets this as an addendum.
- [ ] `ProblemViewModel` builds `state: StateFlow<ProblemUiState>` from `StatementRepository.statement(problem, settings.statementLanguage)` and the reading settings, and implements `ProblemActions`:
  - `setLanguage(StatementLanguage)`, `setTextSize(TextSizePreference)` and `setWidth(StatementWidthPreference)` write through `SettingsRepository`
  - `retry()` forces a refresh
  - `openLink(url)` opens the URL in the system browser for now (047 refines it)
- [ ] `ProblemStatus`:
  - `Loading` (no data, refreshing)
  - `Loaded` (data; `error` over data shows `core/ui`'s `RefreshErrorBanner`)
  - `Error` (no data: `Offline`, `RateLimited`, `Unavailable` or `Rejected`, with Retry)
  - `Unparsable` (no data and `Malformed`): for now an `ErrorState` "Can't show this statement natively" with Retry; 047 replaces it with the WebView
- [ ] `ProblemPane(viewModel, onBack: (() -> Unit)?, modifier)` is the route, and the stateless `ProblemScreen(state, actions, onBack)` is screenshot-tested:
  - top bar: back arrow when `onBack != null`, `<display> · <title>`, EN/RU toggle (`problem_language_<en|ru>`) and a reading menu (`problem_reading`) with text sizes and widths
  - the content is `StatementContent` (044)
  - the pane's content is tagged `problem_view`
- [ ] `ProblemViewModelTest` (fakes for both repositories, Turbine) covers:
  - with a cached statement, the first state is `Loaded`, never `Loading` (the < 100 ms path)
  - switching to `ru` writes the setting and shows the `ru` statement; switching back shows the cached `en` one with no fetch
  - text size and width changes persist and show in state
  - `Retry` forces a refresh
  - `Malformed` without data gives `Unparsable`; with data it gives `Loaded` plus the banner
- [ ] Screenshots: real statements parsed from `testdata/codeforces/pages/` (test dependencies `core/parser` and `core/testing`):
  - `problemView_<device>_<theme>` (2274A, `en`; tablet: column centred at `Comfortable`)
  - `problemView_ru_phone_dark`
  - `problemView_narrow_tablet_dark`
  - `problemView_textLargest_phone_dark`
  - `problemView_font150_phone_dark`
  - `problemView_loading_phone_dark`
  - `problemView_error_phone_dark`
  - `problemView_staleError_phone_dark`
  - `problemView_unparsable_phone_dark`
  - `problemView_readingMenu_phone_dark` (the menu open)

## Out of scope
- WebView fallback, contest materials and Codeforces links in-app (047). Opening the view from a list (048, 049).
- Bookmarks, notes and personal tags (M10), the Home "Continue" card (M4), and sample "send to editor" (M5).

## Notes for next
<!-- builder fills in -->
