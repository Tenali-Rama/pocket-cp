# benchmark

## Rules
- A `com.android.test` module (Macrobenchmark, UiAutomator) against `:app`'s `benchmarkRelease` / `nonMinifiedRelease` builds. It runs only on a device: CI's `perf` job, never the VM (ADR 0013).
- It applies AGP, Kotlin and `androidx.baselineprofile` directly; the build-logic conventions don't cover test modules. Keep its SDK levels in step with `KotlinAndroid.kt`.
- Drive the UI by test tags only (`By.res("nav_<route>")`, the same ids as Maestro), never by text, so phone and tablet layouts both work.
- `scripts/perf-budget-check.sh` finds results by test method name (`startupCold`, `startupWarm`, `tabWalk`) and metric key. Renaming a test or metric means changing the script, its fixtures in `testdata/perf/` and `config/perf-budgets.json` in the same PR.
- A new budgeted benchmark: add the test here, a line in the script's `metrics`, a budget (first green CI run + 25 %), and fixture values.
- Never generate the Baseline Profile locally. Copy it from the `perf` job's `baseline-profile` artifact to `app/src/main/baseline-prof.txt`.
