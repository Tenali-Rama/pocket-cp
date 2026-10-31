---
id: 038
title: Benchmark build types serve debug fixtures; release stays live
milestone: M3
status: done
deps: []
touches: [app/build.gradle.kts, app/src/debug/, app/src/fixture/, app/src/testDebug/, app/CLAUDE.md, app/API.md, benchmark/CLAUDE.md, testdata/README.md, docs/decisions/0015-debug-fixtures-release-live.md]
spec: ["16. Non-Functional", "3. Performance Budgets"]
---

## Goal
CI's `perf` job benchmarks `benchmarkRelease` / `nonMinifiedRelease`. Today these build types bind live Codeforces (ADR 0015), so the CI emulator makes live calls, which breaks "tests never make live network calls". It also adds noise: the cache warm-up (035) re-fetches on every cold start. After this task, every benchmark and Baseline Profile build type binds `FixtureJudgeSource` over the recorded `testdata/codeforces/api/` responses, as debug does (026). Only `release` binds live Codeforces.

## Acceptance criteria
- [ ] The fixture binding moves from `app/src/debug/kotlin/` to `app/src/fixture/kotlin/` (same package and classes, no behaviour change). That dir and the `testdata/codeforces/api` assets are source dirs of `debug` and of every `benchmark*` / `nonMinified*` build type. `src/live/kotlin` is a source dir of `release` only. The `finalizeDsl` check still fails configuration for a build type that binds neither.
- [ ] A new `checkFixtureBinding` task runs in `:app:ciCheck`:
  - it fails when the release APK contains a fixture asset (`assets/contest.list.json`)
  - it fails when the merged assets of `benchmarkRelease` or `nonMinifiedRelease` lack `contest.list.json`

  The PR shows that both checks can fail: with the release source-set line reverted, CI goes red with the task's message (a CI run link or log tail in the PR).
- [ ] `ciCheck` still compiles `BenchmarkRelease` and `NonMinifiedRelease` with their Hilt graph. A duplicate or missing `JudgeSource` binding fails CI.
- [ ] `DebugJudgeWiringTest` and `FixtureJudgeSourceTest` pass unchanged from their new source location.
- [ ] CI's `perf` job is green with the budgets unchanged. The PR body lists the `perf` artifact values (memory, cold and warm start, frame p90) next to main's last run, so the owner sees the shift from live to fixture data.
- [ ] ADR 0015 gets an addendum (≤ 15 lines in total). `app/CLAUDE.md`, `app/API.md`, `benchmark/CLAUDE.md` and `testdata/README.md` say that benchmarks and the Baseline Profile run on fixtures, and that only `release` is live.

## Out of scope
- Editing `.github/workflows/` (token scope), `config/perf-budgets.json` or `app/src/main/baseline-prof.txt`. Re-basing budgets on the new numbers is a later chore under the ADR 0013 rule (≥ 20 runs for timings).
- A larger, perf-sized fixture (for example the full problemset) so benchmarks see realistic list sizes. Note it as a follow-up if the numbers drop a lot.
- Statement page fixtures (041 adds them to the same asset set) and the statement scroll benchmark (049).

## Notes for next
- `app/src/fixture/kotlin/` (+ `testdata/codeforces/api` assets) is wired to `debug` in `sourceSets` and to every `benchmark*` / `nonMinified*` type in `finalizeDsl`; `src/live/kotlin` is `release` only.
- `checkFixtureBinding` walks `build/intermediates/assets/<type>/` (AGP 8.10 merged-assets location) and the release APK; it collects every problem and fails once, so one red run shows both messages.
- The tests stay in `src/testDebug/` (they run against the debug graph); only the bound classes moved.
- Comment-only updates outside `touches`: `LiveJudgeSourceModule.kt` and `DataWiringTest.kt` named `src/debug`.
- Follow-up: perf budgets can be re-based on fixture numbers after ≥ 20 runs (ADR 0013); a perf-sized fixture is still out of scope.
