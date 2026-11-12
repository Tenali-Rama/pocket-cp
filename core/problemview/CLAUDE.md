# core/problemview

## Rules
- The one problem view every feature embeds (Problemset, Contests), as a page or as the detail pane. It lives in
  `core/` because features never depend on each other. No navigation here: the feature owns routes and back. The
  one exception is a Codeforces page open over the statement (`ProblemUiState.webPage`), which its own
  `BackHandler` closes before the feature's back runs.
- Pages from Codeforces (unparsable statement, contest materials, statement links) show only through
  `core/webview`'s `CodeforcesWebView` (ADR 0021); links off Codeforces go to the system browser, `http(s)` only.
- Features build the view model only through `ProblemViewModelFactory.create` (ADR 0012 addendum). A new
  view-model dependency goes into the factory's `@Inject` constructor, never into a feature.
- Reading settings resolve here: `TextSizePreference.scale` (core/datastore) and `StatementWidthPreference.maxWidth()`
  turn preferences into the plain values `core/statement` takes.
- Cache first (ADR 0011): a cached statement is the first state (`SharingStarted.Eagerly`), never a skeleton; a
  failed refresh over data is `RefreshErrorBanner`.
- Tokens only; strings in `res/values/strings.xml`, prefixed `problemview_`.
- Tests: `FakeStatementRepository` and `FakeSettingsRepository` (this module's tests), Turbine for the view model.
  Screenshots use real statements parsed from `testdata/codeforces/pages/` and in-memory images; never the network.
