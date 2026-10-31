---
id: 049
title: Contest problems open statements, contest statement prefetch, statement scroll benchmark, and release 0.3.0
milestone: M3
status: todo
deps: [038, 043, 045, 046, 047, 048]
touches: [feature/contests/, .maestro/contests.yaml, app/build.gradle.kts, benchmark/src/main/kotlin/app/pocketcp/benchmark/StatementBenchmark.kt, benchmark/src/main/kotlin/app/pocketcp/benchmark/BaselineProfileGenerator.kt, benchmark/src/main/kotlin/app/pocketcp/benchmark/Shell.kt, benchmark/CLAUDE.md, scripts/perf-budget-check.sh, scripts/test-perf-budget-check.sh, testdata/perf/, config/perf-budgets.json, app/src/main/baseline-prof.txt, docs/decisions/0013-perf-budgets-on-ci-emulator.md]
spec: ["7.2 Contests", "7.4 Problem View", "3. Performance Budgets", "14. Offline"]
---

## Goal
A contest's problems now open their statements inside the Contests tab. Showing a contest prefetches all of its statements in the user's statement language (spec §3), so they open instantly and stay available offline (spec §14). It also puts the statement screen, the most jank-prone one (native LaTeX + images), under spec §3's CI tracking: a scroll benchmark with a budget, and a statement journey in the Baseline Profile. This is M3's last task: it sets the app version for `v0.3.0`.

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
- [ ] Statement benchmark (spec §3 "no dropped frames when scrolling … statements"; `benchmark/CLAUDE.md` rules). It runs on fixtures (038, ADR 0015) and drives the UI by test tags only:
  - `StatementBenchmark.statementScroll` (`FrameTimingMetric`, 5 iterations like `tabWalk`): the setup starts the app, opens `nav_problemset`, taps `problem_2274A` and waits for `problem_view` (this caches the statement), then taps another fixture problem. The measured block taps `problem_2274A` again (a cache hit), waits for `problem_view`, then flings it down 3 times and back up once. Shared helpers go in `Shell.kt`.
  - `BaselineProfileGenerator.generate` adds the same journey (open 2274A from Problemset, fling down and up) after `walkTabs()`. `app/src/main/baseline-prof.txt` is replaced with this PR's `baseline-profile` artifact (never generated locally, ADR 0013).
  - `scripts/perf-budget-check.sh` gains `statement_frame_p90_ms` (`statementScroll`, `sampledMetrics.frameDurationCpuMs.P90`). `testdata/perf/pass/` and `over/` get a `statementScroll` entry and `testdata/perf/budgets.json` a budget; `scripts/test-perf-budget-check.sh` expects the new `OK` line in both and a `MISSING statement_frame_p90_ms` in the crashed-run case. `scripts/verify.sh` passes.
  - `config/perf-budgets.json` gets `statement_frame_p90_ms` = this PR's first green `perf` value + 25 %, rounded up (ADR 0013: one run for a new timing until it flakes). The PR body gives the run id and the value; ADR 0013 gets a one-line addendum naming it. No other budget changes.
  - Out of reach of this benchmark: the < 100 ms open from cache is pinned by 043/046's tests (the first state from a cached row is `Loaded`, no judge call) and measured on hardware in M14.
- [ ] Release: `appVersionName = "0.3.0"`, `appVersionCode = 4` in `app/build.gradle.kts` (app/CLAUDE.md versioning rule). Any golden that shows the version is re-accepted. The PR is merged after every other M3 task (its deps), and before the `v0.3.0` tag is pushed.

## Out of scope
- Prefetch when a running contest starts while the app is in the background, or of contests whose problems aren't in the problemset yet (M8 standings, recorded on ROADMAP's M8 line). Any change to `core/problemview` or `core/data`.
- Editing `.github/workflows/`: the `perf` job already runs every test in `:benchmark`.

## Notes for next
<!-- builder fills in -->
