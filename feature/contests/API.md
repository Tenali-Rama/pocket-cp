# feature/contests API

The Contests tab: running, upcoming and past Codeforces contests (no gyms), cache first, with live countdowns and division and type filters.

Package `app.pocketcp.feature.contests`. The app adds it with `implementation(project(":feature:contests"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `ContestsEntry` | The `FeatureEntry`: route `contests`, label `R.string.contests_label` ("Contests"), `Icons.Filled.DateRange`, order 10, tag `nav_contests`. Replaces the app's placeholder. Bound `@IntoSet` by `ContestsModule`. Injects `ContestRepository`. |
| `ContestsViewModel(repository, savedState, clock)` | `state: StateFlow<ContestsUiState>` from `contests(gym = false)`; implements `ContestsActions`. Filters persist in `savedState`. |
| `ContestsActions` | `toggleDivision`, `toggleKind`, `clearFilters`, `refresh()` (`refresh(gym = false, force = true)`, with the pull indicator). |
| `ContestsUiState(status, groups, filter, error, userRefreshing)` | `status`: `Loading`, `Error`, `Empty`, `Loaded`. `groups`: non-empty `ContestGroup(section, contests)` in order. |
| `ContestSection` | `Running` (ending soonest first), `Upcoming` (starting soonest first), `Past` (newest first). Contests without a start go last; an `Unknown` phase is in none. |
| `ContestFilter(divisions, kinds)` | Selected chips; empty means all. `KindChoices`: every `ContestKind` but `Unknown`. |

Test tags (Maestro ids): `contests_division_<div1|div2|div3|div4|div1and2|educational|global|other>`, `contests_kind_<cf|icpc|ioi>`, rows `contest_<id>`.

Screenshots: `contests_<device>_<theme>`, `contests_filtered_phone_dark`, `contests_empty_phone_dark`, `contests_error_phone_dark`,
`contests_staleError_phone_dark`, `contests_loading_phone_dark`, `contests_font150_phone_dark`. Maestro: `.maestro/contests.yaml`.
