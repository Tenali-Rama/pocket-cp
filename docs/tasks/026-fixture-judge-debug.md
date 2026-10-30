---
id: 026
title: Debug builds serve Codeforces fixtures so Maestro flows have stable data
milestone: M2
status: done
deps: [024]
touches: [core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesModule.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesModuleTest.kt, core/codeforces/API.md, core/testing/, app/src/debug/, app/src/live/, app/src/testDebug/, app/build.gradle.kts, app/src/test/kotlin/app/pocketcp/data/DataWiringTest.kt, app/CLAUDE.md, app/API.md, docs/decisions/, testdata/README.md]
spec: ["16. Non-Functional", "4. Platform & Architecture"]
---

## Goal
The M2 Maestro flows need real-looking contests and problems, and must not make live network calls. CI's `ui` job installs `app-debug.apk`, and the workflow can't be edited (token scope). So the **debug** build binds a fixture-backed `JudgeSource` that serves `testdata/codeforces/api/`, and the **release** build binds the live Codeforces source (default — owner may change).

**Sequencing with 024.** 024 (benchmark build type, in progress) and this task both edit `app/build.gradle.kts` and `app/CLAUDE.md`, so this task starts after 024 merges. It then gives every build type that 024 adds to `app` (e.g. `benchmark`, or the baseline-profile plugin's `benchmarkRelease` / `nonMinifiedRelease`) the live binding, like release, so benchmarks measure the real mapping path (default — owner may change).

## Acceptance criteria
- [ ] `CodeforcesModule` keeps providing `CodeforcesApi` but no longer binds `JudgeSource`. The `JudgeSource` binding moves to `app`'s build-type source sets:
  - `src/live/kotlin/`: the `CodeforcesJudgeSource` binding. It is added as an extra source dir to `release` and to every build type 024 created, so the binding is written once.
  - `src/debug/`: `FixtureJudgeSource`

  Any later build type has to add one of the two explicitly; record this in `app/CLAUDE.md`.
- [ ] `FixtureJudgeSource` is reachable only from debug code: release and benchmark APKs contain no fixture code or assets. The builder picks where it lives and records the choice in the ADR:
  - (a) `app/src/debug/` as `CodeforcesJudgeSource(CodeforcesApi(<request queue over an asset-reading transport>))` (suggested: no new module edges), or
  - (b) `core/testing` main, used by `app` only as `debugImplementation`. `core/codeforces` already has `testImplementation(project(":core:testing"))`, so this adds a `core/testing -> core/codeforces` edge. Gradle resolves it, but the ADR says why it was worth it.

  Either way it runs the real `CodeforcesJudgeSource` mapping over a transport that reads the fixtures from assets: `contest.list.json`, `contest.list.gym.json`, `problemset.problems.json`, `user.info.json`. The debug build packages these through `sourceSets["debug"].assets` pointing at `testdata/codeforces/api/`; nothing is copied.
- [ ] It shifts every contest start by one offset so that the fixture's recording moment (`startTimeSeconds + relativeTimeSeconds` of any contest) becomes "now". Upcoming contests then show positive countdowns, and phases stay as recorded.
- [ ] `FixtureJudgeSourceTest` (JVM, fixed `Clock`, next to wherever `FixtureJudgeSource` lives) checks that:
  - the shift puts the first upcoming contest at its recorded distance from now
  - the problemset count matches the fixture
  - a missing asset becomes `JudgeError.Malformed`
- [ ] `DataWiringTest` still resolves `ContestRepository` / `ProblemRepository` on the debug variant. ciCheck's unit tests and assemble only cover debug, so `app`'s `ciCheck` also depends on the Kotlin + Hilt compile of release and of each 024 build type (e.g. `compileReleaseJavaWithJavac`, no R8). A broken live binding then fails CI.
- [ ] The existing `smoke.yaml` and `settings.yaml` pass unchanged on the debug APK.
- [ ] A new ADR (next free number, ≥ 0013): debug = fixtures, release = live, and why (no workflow edits, deterministic flows, no live calls).

## Out of scope
- Editing `.github/workflows/*`. Recording new fixtures from the live site (the canary task 009 owns live access).

## Notes for next
- Option (a): `FixtureJudgeSource` lives in `app/src/debug/kotlin/app/pocketcp/judge/` (ADR 0015); no new module edge.
- Its `CodeforcesApi` uses base URL `https://fixtures.invalid/` (not under `/api/`), so the queue doesn't space local reads 2 s apart.
- The recording moment comes from `contest.list.json` (one extra local read, then cached); gyms use the same shift.
- `app/build.gradle.kts` fails configuration if a build type is neither debug, release nor a Baseline Profile type; a new one must pick a binding there.
- `DebugJudgeWiringTest` (testDebug) checks the debug graph binds `FixtureJudgeSource`; `DataWiringTest` stays variant-neutral.
