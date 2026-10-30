# core/ui API

Domain-aware UI shared by Home, Contests and Problemset: contest and problem rows, ticking countdowns (`rememberNow`, `formatCountdown`) and the refresh-error banner.

Package `app.pocketcp.core.ui`. Add with `implementation(project(":core:ui"))`; it exposes `core/model` as `api`.

```kotlin
val now = rememberNow() // once per list, ticks every second
LazyColumn {
    items(contests, key = { it.id.value }) { ContestRow(it, now, onClick = { open(it) }) }
}
```

| Symbol | What it is |
|--------|-----------|
| `formatCountdown(remaining: java.time.Duration): String` | `2d 03:04:05` for a day or more, `03:04:05` below, `00:00:00` when negative. Partial seconds dropped. |
| `LocalClock` | `java.time.Clock` the UI reads; defaults to `Clock.systemDefaultZone()`. Its zone formats dates. Tests provide a fixed one. |
| `rememberNow(clock = LocalClock.current, tick = 1.seconds): Instant` | The clock's instant, re-read every `tick` while composed. |
| `ContestRow(contest, now, onClick, modifier)` | Name, time line and phase label. Upcoming: `Starts in <countdown>`; Running: `Ends in <countdown>`; Finished: localized start date · length (`02:15`); Unknown: no time; `start == null` (not Unknown): `Start time not announced`. Tag `contest_<id>`. |
| `ProblemRow(problem, hideTags, hideRating, highlighted, onClick, modifier)` | `display · name`, tags, rating chip in its rank colour, `N solved`. Hidden parts are absent from the semantics too; `highlighted` → `surfaceRaised` and `selected`. Tag `problem_<display>`. |
| `RefreshErrorBanner(error: JudgeError, onRetry, modifier)` | For a failed refresh over cached data: one message per `JudgeError` case (`Rejected` shows its own message) and a Retry button. Polite live region. |

Rows are ≥ `spacing.touchTarget` tall and activated by `Enter` (`RowsA11yTest`).
Screenshots: `browseRows_*`, `browseRows_hiddenSpoilers_phone_dark`, `browseRows_font150_phone_dark`, `browseRows_font150Problems_phone_dark` (the problem rows the full gallery pushes off a phone at 1.5×).
