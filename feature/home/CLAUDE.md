# feature/home

## Rules
- Contests come from `ContestRepository.contests(gym = false)` only (ADR 0011); this module caches nothing itself.
- Sectioning is pure (`homeUiState`, `homeSections`, `nextCountdownEnd` in `HomeUiState.kt`) and runs off the main
  thread in `HomeViewModel`. Time comes from the injected `Clock`, never `Instant.now()`.
- A countdown reaching zero re-sections at once and starts one forced refresh; never poll on a timer.
- Rows are `core/ui`'s `ContestRow`; one `rememberNow()` per screen drives every countdown.
- Layout follows `LocalWindowLayout`: expanded width shows guest card + Running | Upcoming, narrower one list.
- The guest card (`home_guest`) has no action until sign-in (M4). Never rename a test tag; Maestro uses them.
- The view model is built by `HomeEntry` with `viewModel { }` (ADR 0012); `HomeScreen` stays stateless for screenshots.
- Rating, verdicts (M4), Continue (M3), upsolve and daily suggestion (M10) arrive as their own cards here.
