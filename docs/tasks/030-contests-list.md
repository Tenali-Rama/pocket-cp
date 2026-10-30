---
id: 030
title: Contests feature with running, upcoming and past lists and division/type filters
milestone: M2
status: todo
deps: [025, 026, 027, 028]
touches: [feature/contests/, settings.gradle.kts+, app/build.gradle.kts+, .maestro/contests.yaml]
spec: ["7.2 Contests", "2. Product Principles", "14. Offline"]
---

## Goal
The Contests tab is a native list of running, upcoming and past Codeforces contests (gyms excluded, M8). It is cache-first, with live countdowns and filters by division and type. It replaces the `contests` placeholder by binding the same route (025).

## Acceptance criteria
- [ ] `feature/contests` binds a `contests` `FeatureEntry` (order 10, same label and icon as the placeholder), with its view model built per ADR 0012 from `ContestRepository.contests(gym = false)`.
- [ ] One list in three sections, Running, Upcoming (soonest first) and Past (newest first), built from `core/ui`'s `ContestRow`. An empty section is hidden.
- [ ] Filters:
  - division: multi-select chips over `ContestDivision`
  - type: chips over `ContestKind`, excluding `Unknown`
  - none selected means all
  - filter state survives process death (`SavedStateHandle`)
- [ ] Data states follow ADR 0011:
  - cached data is shown at once
  - `refreshing` with data shows no spinner
  - pull-to-refresh calls `refresh(force = true)`
  - `Cached.error` with data shows `RefreshErrorBanner` above the list
  - no data plus an error shows a full error state with Retry
  - no data while refreshing shows a loading state
  - a filter that leaves nothing shows an empty state
- [ ] `ContestsViewModelTest` (fake repository, Turbine, fixed clock) covers sectioning and order, each filter and their combination, restored filters, and the four data states.
- [ ] Screenshots (fixture data from `contest.list.json`, fixed clock):
  - `contests_<device>_<dark|light>`: all 8
  - phone: single pane with the filter chips visible
  - `contests_filtered_phone_dark`: Div. 2 + CF selected
  - `contests_empty_phone_dark`, `contests_error_phone_dark`, `contests_staleError_phone_dark`
  - `contests_font150_phone_dark`
- [ ] Maestro `.maestro/contests.yaml` (debug fixtures, 026):
  - taps `nav_contests` and asserts `screen_contests`
  - asserts a fixture upcoming contest's row `contest_<id>` and the text "Starts in"
  - selects the Div. 2 chip and asserts a Div. 3 fixture contest is gone
  - takes screenshot `contests_filtered`
- [ ] `feature/contests/CLAUDE.md` and `API.md`.

## Out of scope
- Contest detail (031). Register/unregister, standings, virtual (M8). Gym (M8).

## Notes for next
<!-- builder fills in -->
