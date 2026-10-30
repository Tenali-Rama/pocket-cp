# 0013 — Perf budgets on the CI emulator, Baseline Profile from CI

- **Status**: accepted
- **Context**: Spec §3 wants size, startup, memory and jank tracked in CI with a failing build on regression. CI has only an x86 emulator on shared runners, so it cannot measure the real targets (1 s cold / 300 ms warm on a mid-range tablet).
- **Decision**:
  - CI's `perf` job runs Macrobenchmark (`benchmark/`) on a `pixel_tablet` API 34 emulator with `suppressErrors=EMULATOR`; `scripts/perf-budget-check.sh` compares the results and the release APK size with `config/perf-budgets.json`.
  - Each budget is the first green CI run's value + 25 % headroom (run 41107065675, PR #35). These budgets guard against regressions only; the real §3 targets are checked on hardware in M14.
  - Raise a budget only in a PR that says why the growth is wanted, with the `perf` artifact values. Tighten freely.
  - The Baseline Profile is generated only by CI (`BaselineProfileGenerator`) and committed from the `baseline-profile` artifact as `app/src/main/baseline-prof.txt`. `ciCheck` fails when the release APK lacks `assets/dexopt/baseline.prof`.
  - The plugin-made `benchmarkRelease` / `nonMinifiedRelease` builds are debug-signed so the emulator can install them.
- **Consequences**: Emulator timings are noisy; 25 % headroom absorbs it, but a flaky `OVER` means re-run, then investigate. Don't loosen budgets to make a PR green, and don't generate the profile locally.
