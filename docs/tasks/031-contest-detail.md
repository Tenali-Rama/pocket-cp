---
id: 031
title: Contest detail with timer and problem list, list | detail on wide windows
milestone: M2
status: done
deps: [029, 030]
touches: [feature/contests/, .maestro/contests.yaml]
spec: ["7.2 Contests", "5. Design & Layout", "3. Performance Budgets"]
---

## Goal
Tapping a contest opens its detail: name, phase, timer and the contest's problems. On medium and wider windows it is list | detail, as in Settings.

## Acceptance criteria
- [ ] Route `contests/{id}`, inside `feature/contests`' `register`. Compact width: a full page with a back arrow and `BackHandler`. Medium and wider: contest list | detail panes, with the first contest selected by default (default — owner may change). The selected id survives rotation, resize and process death.
- [ ] Header:
  - name, kind, division and phase
  - upcoming: "Starts in" countdown; running: "Ends in" countdown (`rememberNow`)
  - finished: start date and duration
- [ ] Problems come from `ProblemRepository.problemset()` filtered by `contestId`, ordered by index, and rendered with `ProblemRow`. The spoiler flags come from `UserSettings` (029, read via `SettingsRepository`).
  - No problems, and the contest isn't finished: "Problems appear here once Codeforces adds them to the problemset" (default — owner may change).
  - No problems for a finished contest: an empty state.
- [ ] Prefetch: opening a contest triggers `ProblemRepository.refresh()` (non-forced; the stale rule decides).
- [ ] `ContestDetailViewModelTest` covers the header per phase with a fixed clock, problem filtering and order, both empty states, and a restored selection.
- [ ] Screenshots:
  - `contestDetail_<device>_<dark|light>`: all 8. Phone: detail page; tablet, foldable and desktop: list | detail with the selected row highlighted
  - `contestDetail_upcoming_phone_dark`: countdown and the not-yet-published message
  - `contestDetail_font150_phone_dark`
- [ ] Maestro `contests.yaml` adds these steps:
  - tap a finished fixture contest
  - assert `problem_<id>A`
  - take screenshot `contest_detail`
  - press Back on phone (the flow must also pass on the tablet list | detail)

## Out of scope
- Solve status (M4). Announcements, standings, register (M8). Opening a problem (M3).

## Notes for next
- The opened contest is `ContestDetailViewModel` state (`KEY_SELECTED` in `SavedStateHandle`), not a nav back-stack entry: `register` has no `NavController` (ADR 0016). `contests/{id}` seeds it once. Cross-feature links (Home, M4 deep links) should navigate to `contests/{id}`.
- Wide default = first contest of the list as displayed (after filters), set by `ContestsRoute` through `showByDefault`; compact sets none, so a phone never opens a page by itself.
- Layout: compact shows the page; medium shows chips and list beside detail (1:1); expanded shows filter pane, list and detail (1:2:2). Without a shown contest, wide keeps filter pane beside list.
- `contest.list.json` gained finished Div. 2 round 2274 (its 2274A is in `problemset.problems.json`), so fixture screenshots and Maestro can show problems. Contest ids 2278/2272/2277 keep their order.
- Problem rows have a no-op click until M3 opens problems; solve status is M4.
