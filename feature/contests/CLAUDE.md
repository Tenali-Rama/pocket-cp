# feature/contests

## Rules
- Data comes only from `ContestRepository` and `ProblemRepository` (`core/data`), regular contests only (`gym = false`); gyms are M8. Spoiler flags come from `SettingsRepository` (`core/datastore`). Never cache or fetch here: stale-while-revalidate is ADR 0011. Showing a contest calls `ProblemRepository.refresh()` unforced; only Retry forces.
- Data states follow ADR 0011: cached data at once, no indicator for a background refresh, a `RefreshErrorBanner` over data, a full `ErrorState` only without data, `LoadingPlaceholder` only without data. The pull indicator shows only a refresh the user asked for.
- Sectioning and filtering are pure functions in `ContestsUiState.kt`, tested in `ContestsViewModelTest` with a fixed clock. The view model re-sections when an upcoming contest starts or ends (`nextPhaseChange`), never on a ticking timer.
- Filters live in `SavedStateHandle` as enum names (`KEY_DIVISIONS`, `KEY_KINDS`), so they survive process death. None selected means all.
- Rows are `core/ui`'s `ContestRow`; the list calls `rememberNow()` once (in `ContestsRoute`) and passes `now` down.
- Layout follows the window width class (`LocalWindowLayout`): compact shows the chip rows above the list, or the opened contest's page (back arrow and `BackHandler` close it); medium shows chips and list | detail; expanded shows filter pane | list | detail. Wide windows show the list's first contest until one is opened (`showByDefault`), and filter pane | list while nothing is shown.
- The opened contest lives in `ContestDetailViewModel`'s `SavedStateHandle` (`KEY_SELECTED`), never on the nav back stack (ADR 0016). `contests/{id}` is the same screen, seeded from `id` once.
- The detail header and problem filtering are pure functions in `ContestDetailUiState.kt`, tested in `ContestDetailViewModelTest` with a fixed clock; the view model rebuilds it when the shown contest starts or ends, never on a ticking timer.
- Test tags are Maestro ids, so never rename one: `contests_division_<division>` and `contests_kind_<kind>` (lower-case enum names), `contest_detail`, plus `core/ui`'s `contest_<id>` and `problem_<display>`.
- Both view models are built by `ContestsEntry` with `viewModel { }` (ADR 0012); `ContestsScreen` stays stateless for screenshots, which show `contest.list.json` and `problemset.problems.json` at the list's recording time (`FixtureContests`, `FixtureProblems`).
