---
id: 034
title: Home feature with running and upcoming contests, countdowns and the guest state
milestone: M2
status: todo
deps: [025, 026, 027]
touches: [feature/home/, settings.gradle.kts+, app/build.gradle.kts+, .maestro/home.yaml]
spec: ["7.1 Home", "2. Product Principles", "6. Authentication"]
---

## Goal
Home is the start destination, and it shows what's happening now: running contests and the next upcoming ones, with live countdowns. Until login lands (M4), it shows a guest card. It replaces the `home` placeholder by route (025) and keeps order 0.

## Acceptance criteria
- [ ] `feature/home` binds the `home` `FeatureEntry` (order 0, same label and icon), with its view model per ADR 0012 over `ContestRepository.contests(gym = false)`.
- [ ] Sections:
  - "Running now": all running contests, with "Ends in" countdowns
  - "Upcoming": the next 5, soonest first (default — owner may change), with "Starts in" countdowns
  - each section is hidden when empty; when both are, an empty state says there are no scheduled contests
- [ ] When a countdown reaches zero, the list re-sorts on the next tick: the contest moves from Upcoming to Running locally, without waiting for a refresh. A refresh is also triggered at that moment.
- [ ] Guest card: "Browsing as a guest. Your rating, rank and recent verdicts appear here after you sign in." (default — owner may change). It has no button until M4, and its tag is `home_guest`.
- [ ] Data states as in 030: no spinner over data, banner on a stale error, full error, loading.
- [ ] `HomeViewModelTest` (fixed and advancing test clock) covers sectioning, the limit of 5, the local phase flip at zero plus the triggered refresh, and the data states.
- [ ] Screenshots:
  - `home_<device>_<dark|light>`: all 8, with 1 running and 5 upcoming. On expanded width the two sections sit side by side (default — owner may change)
  - `home_noContests_phone_dark`, `home_error_phone_dark`
  - `home_font150_phone_dark`
- [ ] Maestro `.maestro/home.yaml` (debug fixtures): launches, asserts `screen_home`, `home_guest` and a fixture upcoming `contest_<id>` with "Starts in", and takes screenshot `home`.
- [ ] `feature/home/CLAUDE.md` and `API.md`.

## Out of scope
- Rating/rank and recent verdicts (M4). Continue card (M3). Upsolve preview and daily suggestion (M10). Opening a contest from Home (cross-feature navigation arrives with deep links, M4).

## Notes for next
<!-- builder fills in -->
