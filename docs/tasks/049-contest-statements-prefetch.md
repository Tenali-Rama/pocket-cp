---
id: 049
title: Contest problems open statements, contest statement prefetch, and release 0.3.0
milestone: M3
status: todo
deps: [038, 043, 045, 046, 047, 048]
touches: [feature/contests/, .maestro/contests.yaml, app/build.gradle.kts]
spec: ["7.2 Contests", "7.4 Problem View", "3. Performance Budgets", "14. Offline"]
---

## Goal
A contest's problems now open their statements inside the Contests tab. Showing a contest prefetches all of its statements in the user's statement language (spec §3), so they open instantly and stay available offline (spec §14). This is M3's last task: it sets the app version for `v0.3.0`.

## Acceptance criteria
- [ ] The opened problem lives in `ContestDetailViewModel`'s `SavedStateHandle` (ADR 0016), with `openProblem(ProblemId)` / `closeProblem()` in `ContestDetailActions`. Opening another contest closes it. Problem rows (`problem_<display>`) open it.
- [ ] Layout:
  - Compact: contest page → problem page; back returns to the contest page, then to the list.
  - Medium and expanded: with a problem open, contest detail | statement, the contest list hidden (default — owner may change), and back closes the problem.
- [ ] Prefetch: when the shown contest's problem list is loaded, and again when it gains problems, the view model calls `StatementRepository.prefetch(problems, settings.statementLanguage)` once per (contest, problem set). `ContestsEntry` injects `StatementRepository` and `ProblemViewModelFactory`. `ContestDetailViewModelTest` covers, with a new `FakeStatementRepository` in the module's tests:
  - one prefetch per shown contest
  - a re-prefetch when the problem list grows
  - none for a contest without problems
  - open/close problem, and closing when another contest opens
- [ ] Screenshots:
  - `contestProblem_<device>_<theme>`: phone shows the statement page with a back arrow; wide windows show contest detail | statement
  - `contestProblem_font150_phone_dark`
  - `contestDetail_*` goldens are re-accepted only if they change
- [ ] `.maestro/contests.yaml` adds:
  - open contest `contest_2274`
  - tap `problem_2274A`
  - `problem_view` visible
  - back
  - `contest_detail` visible
- [ ] Release: `appVersionName = "0.3.0"`, `appVersionCode = 4` in `app/build.gradle.kts` (app/CLAUDE.md versioning rule). Any golden that shows the version is re-accepted. The PR is merged after every other M3 task (its deps), and before the `v0.3.0` tag is pushed.

## Out of scope
- Prefetch when a running contest starts while the app is in the background, or of contests whose problems aren't in the problemset yet (M8 standings). Any change to `core/problemview` or `core/data`.

## Notes for next
<!-- builder fills in -->
