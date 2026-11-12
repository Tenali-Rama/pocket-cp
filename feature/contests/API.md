# feature/contests API

The Contests tab: running, upcoming and past Codeforces contests (no gyms), cache first, with live countdowns and division and type filters, and a contest's detail (header, timer and problems) as a page on phones or beside the list.

Package `app.pocketcp.feature.contests`. The app adds it with `implementation(project(":feature:contests"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `ContestsEntry` | The `FeatureEntry`: route `contests`, label `R.string.contests_label` ("Contests"), `Icons.Filled.DateRange`, order 10, tag `nav_contests`. Replaces the app's placeholder. Also registers `contests/{id}` (`id`: `Int`), the same screen with that contest opened (ADR 0016). Bound `@IntoSet` by `ContestsModule`. Injects `ContestRepository`, `ProblemRepository`, `StatementRepository`, `SettingsRepository` and `ProblemViewModelFactory`; a contest's problems open in `core/problemview`'s `ProblemPane`. |
| `ContestsViewModel(repository, savedState, clock)` | `state: StateFlow<ContestsUiState>` from `contests(gym = false)`; implements `ContestsActions`. Filters persist in `savedState`. |
| `ContestsActions` | `toggleDivision`, `toggleKind`, `clearFilters`, `refresh()` (`refresh(gym = false, force = true)`, with the pull indicator). |
| `ContestsUiState(status, groups, filter, error, userRefreshing)` | `status`: `Loading`, `Error`, `Empty`, `Loaded`. `groups`: non-empty `ContestGroup(section, contests)` in order. |
| `ContestSection` | `Running` (ending soonest first), `Upcoming` (starting soonest first), `Past` (newest first). Contests without a start go last; an `Unknown` phase is in none. |
| `ContestFilter(divisions, kinds)` | Selected chips; empty means all. `KindChoices`: every `ContestKind` but `Unknown`. |
| `ContestDetailViewModel(contests, problems, statements, settings, savedState, clock)` | `state: StateFlow<ContestDetailUiState>`; implements `ContestDetailActions`. The opened contest and problem persist in `savedState`. `showByDefault(id?)`: the contest shown while none is opened (wide windows). Prefetches the problemset (`refresh()`, unforced) when the shown contest changes, and the shown contest's statements (`StatementRepository.prefetch`, statement language) once per contest, problem set and language. |
| `ContestDetailActions` | `open(ContestId)` (another contest closes the problem), `close()`, `openProblem(ProblemId)` (opens its contest too), `closeProblem()`, `retryProblems()` (`refresh(force = true)`). |
| `ContestDetailUiState(selected, shown, status, contest, timing, problemsStatus, problems, problemsError, hideTags, hideRatings, problem)` | `status`: `None`, `Loading`, `NotFound`, `Loaded`. `contest` is as of now (a started upcoming contest is running). `problems`: the contest's, by index. `problem`: the opened problem of `selected`, or `null`. |
| `ContestTiming` | `StartsIn(start)`, `EndsIn(end)`, `Held(start, length)`, `NotAnnounced`, `None` (unknown phase). |
| `ProblemsStatus` | `Loading`, `Error` (no cache, failed), `NotYetAdded` (not finished, none yet), `Empty` (finished, none), `Loaded`. |

Test tags (Maestro ids): `contests_division_<div1|div2|div3|div4|div1and2|educational|global|other>`, `contests_kind_<cf|icpc|ioi>`, rows `contest_<id>`, the detail's content `contest_detail`, problem rows `problem_<display>`; the statement is `core/problemview`'s `problem_view`.

Screenshots: `contests_<device>_<theme>`, `contests_filtered_phone_dark`, `contests_empty_phone_dark`, `contests_error_phone_dark`,
`contests_staleError_phone_dark`, `contests_loading_phone_dark`, `contests_font150_phone_dark`; `contestDetail_<device>_<theme>`,
`contestDetail_upcoming_phone_dark`, `contestDetail_empty_phone_dark`, `contestDetail_loading_phone_dark`, `contestDetail_error_phone_dark`,
`contestDetail_font150_phone_dark`, `contestProblem_<device>_<theme>`, `contestProblem_font150_phone_dark`. Maestro: `.maestro/contests.yaml`.
