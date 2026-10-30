---
id: 027
title: core/ui with contest and problem rows, ticking countdowns and the refresh-error banner
milestone: M2
status: todo
deps: []
touches: [core/ui/, settings.gradle.kts+, gradle/libs.versions.toml]
spec: ["7.1 Home", "7.2 Contests", "7.3 Problemset", "5. Design & Layout"]
---

## Goal
Home, Contests and Problemset share their domain-aware UI pieces through a new `core/ui` module, since features can't depend on each other. It depends on `core/model` and `core/designsystem` only.

## Acceptance criteria
- [ ] `formatCountdown(Duration)` gives `2d 03:04:05` for 1 day or more, `03:04:05` below that, and `00:00:00` for negative values. `CountdownTest` covers each case plus the 1-day boundary.
- [ ] `rememberNow(clock: Clock = LocalClock.current, tick = 1.seconds): Instant` re-reads the clock every tick while it is composed. `LocalClock` defaults to the system clock, and tests provide a fixed one. `NowTest` uses a test clock and checks that the value advances with the main clock.
- [ ] `ContestRow(contest, now, onClick)`:
  - Upcoming: "Starts in <countdown>"
  - Running: "Ends in <countdown>"
  - Finished: start date + duration
  - Unknown: no time
  - `Contest.start == null`: "Start time not announced" (default — owner may change)

  Phase is shown as a text label, not by colour alone. Row tag: `contest_<id>`.
- [ ] `ProblemRow(problem, hideTags, hideRating, highlighted, onClick)` shows the `display` id, the name, the rating chip in its rank colour (via the design system's `Rank`), the tags and the solved count. With `hideRating` the chip is gone, and with `hideTags` the tags are gone, including from the content description. Row tag: `problem_<display>`.
- [ ] `RefreshErrorBanner(error: JudgeError, onRetry)` has one message per `JudgeError` case (`Rejected` shows its message) and a Retry button. `RefreshErrorBannerTest` asserts each message.
- [ ] Tokens only. Every row is ≥ `spacing.touchTarget` tall, focusable and clickable with Enter (`RowsA11yTest`).
- [ ] Screenshots of a gallery with one row per contest state, 3 problem rows and the banner:
  - `browseRows_<device>_<dark|light>`: all 8
  - `browseRows_hiddenSpoilers_phone_dark`: no tags, no rating
  - `browseRows_font150_phone_dark`: nothing clipped
- [ ] `core/ui/CLAUDE.md` and `API.md`.

## Out of scope
- Solve-status badges (need the account, M4). Navigation to a problem (M3).

## Notes for next
<!-- builder fills in -->
