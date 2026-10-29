---
id: 024
title: Baseline Profile, Macrobenchmark and perf budget gate
milestone: M1
status: todo
deps: []
touches: [benchmark/, app/build.gradle.kts, app/src/main/baseline-prof.txt, app/CLAUDE.md, settings.gradle.kts+, build.gradle.kts+, gradle/libs.versions.toml, config/perf-budgets.json, scripts/perf-budget-check.sh, scripts/test-perf-budget-check.sh, testdata/perf/, scripts/verify.sh, .github/workflows/ci.yml, docs/decisions/]
spec: ["3. Performance Budgets"]
---

## Goal
CI tracks app size, startup, memory and jank (spec §3), and fails a PR when one of them regresses past its budget. The app ships a Baseline Profile.

Risk: pushing `.github/workflows/ci.yml` needs a token with the `workflow` scope. If the push is rejected for that, stop and report blocked, as with 009.

## Acceptance criteria
- [ ] A `benchmark/` module (`com.android.test`, Macrobenchmark) contains:
  - `StartupBenchmark`: cold and warm, `StartupTimingMetric` + `MemoryUsageMetric`
  - `ShellJankBenchmark`: switches through every tab by its `nav_*` tag, `FrameTimingMetric`
  - `BaselineProfileGenerator`: launch + tab walk
- [ ] `app` applies the `androidx.baselineprofile` plugin and `profileinstaller`. The generated profile is committed as `app/src/main/baseline-prof.txt`, taken from the CI `baseline-profile` artifact and never generated locally. `ciCheck` fails when the release APK has no `assets/dexopt/baseline.prof`.
- [ ] The budgets live in `config/perf-budgets.json`:
  - release APK bytes
  - cold and warm startup median ms
  - frame duration p90 ms
  - max memory KB

  Each is set from the first green CI run + 25 % headroom on the CI emulator (default — owner may change). `scripts/perf-budget-check.sh <results-dir>` reads the Macrobenchmark JSON and the APK size, prints one line per metric (`OK|OVER name value/budget`) and exits 1 on any `OVER`.
- [ ] A new `perf` job in `ci.yml` (tablet emulator, API 34) runs the benchmarks and the check, and uploads the `perf` and `baseline-profile` artifacts. `scripts/test-perf-budget-check.sh` runs on two sample results dirs in `testdata/perf/`: one passes and one fails. `scripts/verify.sh` runs it.
- [ ] Proof in the PR: a temporary commit that lowers one budget makes `perf` fail with `OVER`; it is reverted before merge (link both runs).
- [ ] An ADR records the budget mechanism: emulator budgets guard against regressions, and the real §3 targets are checked on device in M14.

## Out of scope
- List and statement scroll benchmarks (with the lists, M2/M3). Editor latency (M5). Meeting the 1 s / 300 ms targets on hardware (M14).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
