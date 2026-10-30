---
id: 026
title: Debug builds serve Codeforces fixtures so Maestro flows have stable data
milestone: M2
status: todo
deps: []
touches: [core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesModule.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesModuleTest.kt, core/codeforces/API.md, core/testing/, app/src/debug/, app/src/release/, app/build.gradle.kts, app/src/test/kotlin/app/pocketcp/data/DataWiringTest.kt, app/CLAUDE.md, app/API.md, docs/decisions/, testdata/README.md]
spec: ["16. Non-Functional", "4. Platform & Architecture"]
---

## Goal
The M2 Maestro flows need real-looking contests and problems, and must not make live network calls. CI's `ui` job installs `app-debug.apk`, and the workflow can't be edited (token scope). So the **debug** build binds a fixture-backed `JudgeSource` that serves `testdata/codeforces/api/`, and the **release** build binds the live Codeforces source (default — owner may change).

## Acceptance criteria
- [ ] `CodeforcesModule` keeps providing `CodeforcesApi` but no longer binds `JudgeSource`. The `JudgeSource` binding moves to `app`'s build-type source sets:
  - `src/release/`: `CodeforcesJudgeSource`
  - `src/debug/`: `FixtureJudgeSource`

  Any later build type (e.g. 024's benchmark) has to pick one explicitly; record this in `app/CLAUDE.md`.
- [ ] `FixtureJudgeSource` lives in `core/testing` main, and `app` uses it only as `debugImplementation`. It runs the real `CodeforcesJudgeSource` mapping over a transport that reads the fixtures from assets: `contest.list.json`, `contest.list.gym.json`, `problemset.problems.json`, `user.info.json`. The debug build packages these through `sourceSets["debug"].assets` pointing at `testdata/codeforces/api/`; nothing is copied.
- [ ] It shifts every contest start by one offset so that the fixture's recording moment (`startTimeSeconds + relativeTimeSeconds` of any contest) becomes "now". Upcoming contests then show positive countdowns, and phases stay as recorded.
- [ ] `FixtureJudgeSourceTest` (core/testing, JVM, fixed `Clock`) checks that:
  - the shift puts the first upcoming contest at its recorded distance from now
  - the problemset count matches the fixture
  - a missing asset becomes `JudgeError.Malformed`
- [ ] `DataWiringTest` still resolves `ContestRepository` / `ProblemRepository` on the debug variant. ciCheck's unit tests and assemble only cover debug, so `app`'s `ciCheck` also depends on the release Kotlin + Hilt compile (e.g. `compileReleaseJavaWithJavac`, no R8). A broken release binding then fails CI.
- [ ] The existing `smoke.yaml` and `settings.yaml` pass unchanged on the debug APK.
- [ ] A new ADR (next free number, ≥ 0013): debug = fixtures, release = live, and why (no workflow edits, deterministic flows, no live calls).

## Out of scope
- Editing `.github/workflows/*`. Recording new fixtures from the live site (the canary task 009 owns live access).

## Notes for next
<!-- builder fills in -->
