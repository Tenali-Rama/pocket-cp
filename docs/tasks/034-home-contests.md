---
id: 034
title: Home feature with running and upcoming contests, countdowns and the guest state
milestone: M2
status: done
deps: [025, 026, 027, 037]
touches: [feature/home/, settings.gradle.kts+, app/build.gradle.kts+, .maestro/home.yaml, app/src/main/baseline-prof.txt, config/perf-budgets.json]
spec: ["7.1 Home", "2. Product Principles", "6. Authentication"]
---

## Goal
Home is the start destination, and it shows what's happening now: running contests and the next upcoming ones, with live countdowns. Until login lands (M4), it shows a guest card. It replaces the `home` placeholder by route (025) and keeps order 0.

## Acceptance criteria
- [ ] `feature/home` binds the `home` `FeatureEntry` (order 0, same label and icon), with its view model per ADR 0012 over `ContestRepository.activeContests(upcomingLimit = 5)` (037). Home never collects `ContestRepository.contests(...)` and never holds finished contests.
- [ ] Sections:
  - "Running now": all running contests, with "Ends in" countdowns
  - "Upcoming": the next 5, soonest first (default — owner may change), with "Starts in" countdowns. After a local flip at zero, Upcoming may show 4 until the refresh brings the next contest into the bounded query (default — owner may change).
  - each section is hidden when empty; when both are, an empty state says there are no scheduled contests
- [ ] When a countdown reaches zero, the list re-sorts on the next tick: the contest moves from Upcoming to Running locally, without waiting for a refresh. A refresh is also triggered at that moment.
- [ ] Guest card: "Browsing as a guest. Your rating, rank and recent verdicts appear here after you sign in." (default — owner may change). It has no button until M4, and its tag is `home_guest`.
- [ ] Data states as in 030: no spinner over data, banner on a stale error, full error, loading.
- [ ] `HomeViewModelTest` (fixed and advancing test clock) covers sectioning, the limit of 5, the local phase flip at zero plus the triggered refresh, and the data states. Its `FakeContestRepository.contests(...)` fails the test if called, and the test asserts that `activeContests` was requested with `upcomingLimit = 5`.
- [ ] Memory budget (ADR 0013): `config/perf-budgets.json` stays as on main (`memory_max_kb` 84450). Re-run `perf` with the bounded source. Raise the budget only if the gated `memory_max_kb` still exceeds 84450 kB. A raise then needs, in the PR body:
  - the `perf` artifact values (warm and cold medians) of every run
  - where the remaining memory goes, e.g. a `perf` run with Home's collection stubbed
  - why that growth is wanted

  The addendum's "observed max + 15 %" rule is for flaky timings; it does not apply to memory.
- [ ] Screenshots:
  - `home_<device>_<dark|light>`: all 8, with 1 running and 5 upcoming. On expanded width the two sections sit side by side (default — owner may change)
  - `home_noContests_phone_dark`, `home_error_phone_dark`
  - `home_font150_phone_dark`
- [ ] Maestro `.maestro/home.yaml` (debug fixtures): launches, asserts `screen_home`, `home_guest` and a fixture upcoming `contest_<id>` with "Starts in", and takes screenshot `home`.
- [ ] `feature/home/CLAUDE.md` and `API.md`.

## Out of scope
- Rating/rank and recent verdicts (M4). Continue card (M3). Upsolve preview and daily suggestion (M10). Opening a contest from Home (cross-feature navigation arrives with deep links, M4).

## Notes for next
- Home collects `activeContests(upcomingLimit = 5)` only; the test fake fails on `contests(...)`. Sectioning stays pure in `HomeUiState.kt` and still applies `Contest.asOf(now)` (duplicated with `feature/contests`; move it to `core/model` when a third user appears).
- At a countdown's zero Home re-sections locally and calls `refresh(gym = false, force = true)` (forced: the cache is usually < 5 min old). A running contest's end also refreshes; it stays Running until the judge says otherwise.
- Expanded width: guest card + Running on the left, Upcoming on the right; a lone section takes the right pane. Narrower widths show one list.
- Perf (bounded view): `memory_max_kb` 73728 / 71434 kB, under 84450, unchanged. `release_apk_bytes` raised by the measured +20,876 B of this module (#46's rule).
- The guest card (`home_guest`) shows in every state; M4 replaces it. Row `onClick` is a no-op until deep links (M4).
