# 0015 — Debug builds serve fixtures, release builds go live

- **Status**: accepted
- **Context**: CI's `ui` job installs `app-debug.apk` and the workflow can't be edited (token scope). Maestro flows need real-looking contests and problems, deterministic between runs, and must not call Codeforces.
- **Decision**: `core/codeforces` binds no `JudgeSource`. `app/src/debug/` binds `FixtureJudgeSource`: the real `CodeforcesJudgeSource` over a `CodeforcesApi` whose transport reads `testdata/codeforces/api/`, packaged as the debug assets (nothing copied). Contest starts move so the recording moment is now. `app/src/live/kotlin/` binds live Codeforces for `release` and the Baseline Profile build types, so benchmarks measure the real path.
- **Where**: option (a), in `app/src/debug/`, not `core/testing`: no new module edge, and `core/testing` stays test-only.
- **Consequences**: debug installs never show live data; try live behaviour on a release build. Every new build type must pick a binding (the build fails otherwise). `ciCheck` compiles each release-like type with Hilt, so a broken live binding fails CI. Fixture edits change what the debug app and the flows show.
