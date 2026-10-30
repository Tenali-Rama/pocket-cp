# core/ui

## Rules
- Domain-aware UI shared by the Browse features (Home, Contests, Problemset), which can't depend on each other. Depends on `core/model` and `core/designsystem` only; no ViewModels, repositories or navigation here.
- Generic, domain-free components belong in `core/designsystem`; put a piece here only when it takes a `core/model` type.
- Tokens only: no colour, size or string literals (strings live in `res/values/strings.xml`, prefixed `ui_`). Phases and states are words, never colour alone.
- Time comes from `LocalClock`; never call `Instant.now()` or `System.currentTimeMillis()` in a composable. A list calls `rememberNow()` once and passes `now` down to its rows.
- Dates use `LocalClock.current.zone`, so tests pin both time and zone with `TestClock`.
- Rows stay ≥ `spacing.touchTarget` tall in both densities and are clickable with `Enter` (`RowsA11yTest`).
- A new or changed piece appears in `BrowseRowsGallery` (`src/test/`); re-accept the `browseRows_*` screenshots. Galleries pass a fixed `now`: a ticking `rememberNow()` never lets a capture settle.
