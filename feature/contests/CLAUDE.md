# feature/contests

## Rules
- Data comes only from `ContestRepository` (`core/data`), regular contests only (`gym = false`); gyms are M8. Never cache or fetch here: stale-while-revalidate is ADR 0011.
- Data states follow ADR 0011: cached data at once, no indicator for a background refresh, a `RefreshErrorBanner` over data, a full `ErrorState` only without data, `LoadingPlaceholder` only without data. The pull indicator shows only a refresh the user asked for.
- Sectioning and filtering are pure functions in `ContestsUiState.kt`, tested in `ContestsViewModelTest` with a fixed clock. The view model re-sections when an upcoming contest starts or ends (`nextPhaseChange`), never on a ticking timer.
- Filters live in `SavedStateHandle` as enum names (`KEY_DIVISIONS`, `KEY_KINDS`), so they survive process death. None selected means all.
- Rows are `core/ui`'s `ContestRow`; the list calls `rememberNow()` once (in `ContestsRoute`) and passes `now` down.
- Layout follows the window width class (`LocalWindowLayout`): compact shows the chip rows above the list; medium and wider show filter pane | list.
- Test tags are Maestro ids, so never rename one: `contests_division_<division>` and `contests_kind_<kind>` (lower-case enum names), plus `core/ui`'s `contest_<id>`.
- The view model is built by `ContestsEntry` with `viewModel { }` (ADR 0012); `ContestsScreen` stays stateless for screenshots, which show `contest.list.json` at its recording time (`FixtureContests`).
