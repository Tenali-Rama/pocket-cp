# feature/problemset API

The Problemset tab: the whole cached problemset, searchable by name or ID (`1900A`, `1900`), sortable, filterable by tags, rating range and contest division, with a random pick and tags and ratings hideable against spoilers (spec §7.3).

Package `app.pocketcp.feature.problemset`. The app adds it with `implementation(project(":feature:problemset"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `ProblemsetEntry` | The `FeatureEntry`: route `problemset` (replaces the placeholder), label `R.string.problemset_label` ("Problemset"), `Icons.AutoMirrored.Filled.List`, order 20, tag `nav_problemset`. Bound `@IntoSet` by `ProblemsetModule`. Injects `ProblemRepository`, `ContestRepository` and `SettingsRepository`. |
| `ProblemsetViewModel(problems, contests, settings, savedState, computeDispatcher = Dispatchers.Default, random = Random.Default)` | `state: StateFlow<ProblemsetUiState>`, `searchText: String` (Compose state for the field); implements `ProblemsetActions`. Divisions from `contests.contests(gym = false)`. |
| `ProblemsetActions` | `search(text)`, `sortBy(ProblemSort)`, `setHideTags(Boolean)`, `setHideRatings(Boolean)` (write through `SettingsRepository`), `toggleTag(tag)`, `toggleDivision(ContestDivision)`, `setRatingRange(min, max)` (snapped), `clearFilters()`, `pickRandom()`, `refresh()` (forced). |
| `ProblemsetUiState(status, problems, options, error, userRefreshing, tags, pick)` | `status`: `ProblemsetStatus` `Loading`, `Error`, `Empty`, `Loaded`. `tags`: every cached (and selected) tag, sorted. `canPickRandom`: the list is not empty. |
| `ProblemsetOptions(query, sort, hideTags, hideRatings, filter)` | `effectiveSort` (Newest instead of a rating order while ratings are hidden), `sortChoices`. |
| `ProblemFilter(tags, ratingMin, ratingMax, divisions)` | `matches(problem, divisionOf: Map<Int, ContestDivision>)`: every tag (AND); rating in range, unrated only while the range is the full `MIN_RATING` 800 … `MAX_RATING` 3500 (`RATING_STEP` 100); division of a cached contest, uncached contests only with no division selected. `activeCount`, `ratingNarrowed`, `snapRange(min, max)`. |
| `RandomPick(display, count)` | The highlighted problem (`ProblemRow(highlighted = true)`) and the number of picks; each new pick scrolls to its row. |
| `ProblemQuery(text)` | `matches(problem)`: trimmed, case-insensitive; `^\d+[A-Za-z]\d*$` or `^\d+$` match by `display` prefix, anything else by name substring. |
| `ProblemSort` | `Newest` (default), `RatingAscending`, `RatingDescending`, `MostSolved`, `Name`. Unrated problems sort last in both rating orders. |

Test tags (Maestro ids): `problemset_search`, `problemset_search_clear`, `problemset_menu`, `problemset_hide_tags` and `problemset_hide_ratings` (popup: Compose tests only),
`problemset_sort_<newest|ratingascending|ratingdescending|mostsolved|name>`, `problemset_filters`, `problemset_filters_clear`, `problemset_filters_done`, `problemset_filter_sheet`,
`problemset_rating`, `problemset_show_tags`, `problemset_tag_<tag, spaces as _>`, `problemset_division_<div1|div2|div3|div4|div1and2|educational|global|other>`, `problemset_random`, and `core/ui`'s `problem_<display>`.

Screenshots: `problemset_<device>_<theme>`, `problemset_search_phone_dark`, `problemset_hiddenSpoilers_phone_dark`, `problemset_empty_phone_dark`,
`problemset_error_phone_dark`, `problemset_staleError_phone_dark`, `problemset_loading_phone_dark`, `problemset_font150_phone_dark`, `problemset_random_phone_dark`,
`problemsetFilters_<device>_<theme>`, `problemsetFilters_font150_phone_dark`. Maestro: `.maestro/problemset.yaml`.
