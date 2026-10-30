# 0013 — Perf budgets on the CI emulator, Baseline Profile from CI

- **Status**: accepted
- **Context**: Spec §3 wants size, startup, memory and jank tracked in CI with a failing build on regression. CI has only an x86 emulator on shared runners, so it cannot measure the real targets (1 s cold / 300 ms warm on a mid-range tablet).
- **Decision**:
  - CI's `perf` job runs Macrobenchmark (`benchmark/`) on a `pixel_tablet` API 34 emulator with `suppressErrors=EMULATOR`; `scripts/perf-budget-check.sh` compares the results and the release APK size with `config/perf-budgets.json`.
  - Each budget is the first green CI run's value + 25 % headroom (run 41107065675, PR #35). These budgets guard against regressions only; the real §3 targets are checked on hardware in M14.
  - Raise a budget only in a PR that says why the growth is wanted, with the `perf` artifact values. Tighten freely.
  - The Baseline Profile is generated only by CI (`BaselineProfileGenerator`) and committed from the `baseline-profile` artifact as `app/src/main/baseline-prof.txt`. `ciCheck` fails when the release APK lacks `assets/dexopt/baseline.prof`.
  - The plugin-made `benchmarkRelease` / `nonMinifiedRelease` builds are debug-signed so the emulator can install them.
- **Addendum (2026-10-31)**: one run + 25 % was too tight for cold start: it went red on 4 of 33 `perf` attempts (1289.5 / 1312.6 / 1334.4 / 1366.9 ms vs 1283) with no startup change, each green on re-run. A timing budget now comes from ≥ 20 `perf` attempts (red ones included): observed max + 15 %, rounded up. Cold start: n = 33, 752–1367 ms (median 1026) → 1572. Warm start kept 707 (0 of 33 red, max 680 ms). Apply the rule to another timing only when it flakes.
- **Consequences**: Emulator timings are noisy; the headroom absorbs it, but a flaky `OVER` means re-run, then investigate. A single cold-start run reliably catches only regressions of ≥ ~+820 ms (~+80 %); +500 ms trips about 40 % of runs and +300 ms about 18 %, so a green `perf` job is not evidence of no regression. Follow-up: gate on the min or median of 2 runs, or retry once on `OVER`, to sharpen this. Don't loosen budgets to make a PR green, and don't generate the profile locally.
