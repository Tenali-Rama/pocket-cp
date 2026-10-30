# feature/problemset API

The Problemset tab: the whole cached problemset, searchable by name or ID (`1900A`, `1900`), sortable, with tags and ratings hideable against spoilers (spec §7.3).

Package `app.pocketcp.feature.problemset`. The app adds it with `implementation(project(":feature:problemset"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `ProblemsetEntry` | The `FeatureEntry`: route `problemset` (replaces the placeholder), label `R.string.problemset_label` ("Problemset"), `Icons.AutoMirrored.Filled.List`, order 20, tag `nav_problemset`. Bound `@IntoSet` by `ProblemsetModule`. Injects `ProblemRepository` and `SettingsRepository`. |
| `ProblemsetViewModel(problems, settings, savedState, computeDispatcher = Dispatchers.Default)` | `state: StateFlow<ProblemsetUiState>`, `searchText: String` (Compose state for the field); implements `ProblemsetActions`. |
| `ProblemsetActions` | `search(text)`, `sortBy(ProblemSort)`, `setHideTags(Boolean)`, `setHideRatings(Boolean)` (write through `SettingsRepository`), `refresh()` (forced). |
| `ProblemsetUiState(status, problems, options, error, userRefreshing)` | `status`: `ProblemsetStatus` `Loading`, `Error`, `Empty`, `Loaded`. |
| `ProblemsetOptions(query, sort, hideTags, hideRatings)` | `effectiveSort` (Newest instead of a rating order while ratings are hidden), `sortChoices`. |
| `ProblemQuery(text)` | `matches(problem)`: trimmed, case-insensitive; `^\d+[A-Za-z]\d*$` or `^\d+$` match by `display` prefix, anything else by name substring. |
| `ProblemSort` | `Newest` (default), `RatingAscending`, `RatingDescending`, `MostSolved`, `Name`. Unrated problems sort last in both rating orders. |

Test tags (Maestro ids): `problemset_search`, `problemset_search_clear`, `problemset_menu`, `problemset_hide_tags`, `problemset_hide_ratings`,
`problemset_sort_<newest|ratingascending|ratingdescending|mostsolved|name>`, and `core/ui`'s `problem_<display>`.

Screenshots: `problemset_<device>_<theme>`, `problemset_search_phone_dark`, `problemset_hiddenSpoilers_phone_dark`, `problemset_empty_phone_dark`,
`problemset_error_phone_dark`, `problemset_staleError_phone_dark`, `problemset_loading_phone_dark`, `problemset_font150_phone_dark`. Maestro: `.maestro/problemset.yaml`.
