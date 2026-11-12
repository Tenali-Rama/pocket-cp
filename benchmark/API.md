# benchmark API

Macrobenchmarks (startup, memory, tab-switch and statement-scroll jank) and the Baseline Profile generator for `:app`, run by CI's `perf` job against the spec §3 budgets in `config/perf-budgets.json`.

Package `app.pocketcp.benchmark`. Nothing depends on this module; `app` names it as its `baselineProfile` producer.

| Test | Metrics | Budgeted as |
|------|---------|-------------|
| `StartupBenchmark.startupCold` | `StartupTimingMetric`, `MemoryUsageMetric(Max)`; 10 runs, Baseline Profile required | `startup_cold_median_ms` (`timeToInitialDisplayMs` median), `memory_max_kb` |
| `StartupBenchmark.startupWarm` | same | `startup_warm_median_ms`, `memory_max_kb` |
| `ShellJankBenchmark.tabWalk` | `FrameTimingMetric`; fresh process, then every `nav_*` tab; 5 runs | `frame_duration_p90_ms` (`frameDurationCpuMs` P90) |
| `StatementBenchmark.statementScroll` | `FrameTimingMetric`; fresh process, Problemset opens 2274A (caching it) and 2273D, then measured: 2274A from the cache, flung down 3 times and up once; 5 runs | `statement_frame_p90_ms` (`frameDurationCpuMs` P90) |
| `BaselineProfileGenerator.generate` | launch + tab walk + the 2274A statement opened from Problemset and flung down and up | the `baseline-profile` artifact |

`memory_max_kb` is the higher `memoryRssAnonMaxKb` median of the two startup benchmarks. `release_apk_bytes` is the size of `:app:assembleRelease`'s APK.

## CI
- `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark` runs the benchmarks.
- `./gradlew :app:generateBaselineProfile` runs the generator.
- `scripts/perf-budget-check.sh <results-dir> [budgets.json]` prints `OK|OVER name value/budget` (or `MISSING name`) per budget and exits 1 on any `OVER` or `MISSING`. `<results-dir>` holds `*benchmarkData.json` and `apk-bytes.txt`.
- Artifacts: `perf` (results + APK size) and `baseline-profile`.
