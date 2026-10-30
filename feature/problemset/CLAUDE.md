# feature/problemset

## Rules
- Data comes only from `ProblemRepository` (`core/data`); never cache or fetch here (ADR 0011). The spoiler toggles live in `SettingsRepository` (`hideProblemTags`, `hideProblemRatings`); this module stores nothing itself.
- Data states follow ADR 0011: cached data at once, no indicator for a background refresh, a `RefreshErrorBanner` over data, a full `ErrorState` only without data, `LoadingPlaceholder` only without data. The pull indicator shows only a refresh the user asked for.
- Searching and sorting are pure (`ProblemQuery`, `searchProblems` in `ProblemQuery.kt`, `problemsetUiState`), tested in `ProblemQueryTest`, and run on the view model's injected `computeDispatcher` (`Dispatchers.Default`), never on main.
- The query and sort live in `SavedStateHandle` (`KEY_QUERY`, `KEY_SORT` as the enum name), so they survive process death. The text field reads `searchText`, Compose state set on each keystroke, never the async `state`.
- With ratings hidden the rating orders are not offered and the list falls back to Newest (`ProblemsetOptions.effectiveSort`); the chosen order returns when ratings are shown again.
- Rows are `core/ui`'s `ProblemRow` with both toggles applied to every row.
- Layout follows the window width class (`LocalWindowLayout`): compact shows the search field and sort chips above the list; medium and wider show search pane | list.
- Test tags are Maestro ids, so never rename one: `problemset_search`, `problemset_search_clear`, `problemset_menu`, `problemset_hide_tags`, `problemset_hide_ratings`, `problemset_sort_<lower-case ProblemSort>`, plus `core/ui`'s `problem_<display>`.
- The view model is built by `ProblemsetEntry` with `viewModel { }` (ADR 0012); `ProblemsetScreen` stays stateless for screenshots, which show `problemset.problems.json` (`FixtureProblems`).
