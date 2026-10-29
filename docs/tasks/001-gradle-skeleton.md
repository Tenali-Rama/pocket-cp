---
id: 001
title: Gradle project, convention plugins, app module and ciCheck
milestone: M0
status: todo
deps: []
touches: [settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/, gradlew, gradlew.bat, build-logic/, config/detekt/, .editorconfig, .gitignore, app/build.gradle.kts, app/src/main/, app/src/test/, app/CLAUDE.md, app/API.md, docs/decisions/]
spec: ["4. Platform & Architecture"]
---

## Goal
A buildable Gradle project: version catalog, wrapper, `build-logic/` convention plugins for `core/` and `feature/` modules, a Hilt-enabled `app` module that launches, and the aggregate `ciCheck` task that `scripts/verify.sh --full` runs.

## Acceptance criteria
- [ ] CI `verify` job is green and runs `./gradlew ciCheck`, which depends on ktlint, detekt, Android lint, unit tests and `assembleDebug` for every module (wired per module by the convention plugins, so new modules join automatically).
- [ ] Convention plugins exist in `build-logic/`: `pocketcp.android.application`, `pocketcp.android.library`, `pocketcp.android.compose`, `pocketcp.android.feature`, `pocketcp.hilt`, `pocketcp.jvm.library`. minSdk 26, current targetSdk, JDK 17.
- [ ] detekt config has `UnsafeCallOnNullableType` active for main sources (the no-`!!` rule) and is excluded for tests.
- [ ] Robolectric test `MainActivityTest` launches `MainActivity` (Hilt `@AndroidEntryPoint`, `@HiltAndroidApp` application) and asserts the app name text is shown; passes in CI.
- [ ] `app-debug.apk` is produced by CI (the `ui` job's `:app:assembleDebug` step succeeds).
- [ ] Gradle wrapper committed (`gradle/wrapper/`, `gradlew`, `gradlew.bat`), executable bit set on `gradlew`.
- [ ] `app/CLAUDE.md`, `app/API.md`, `build-logic/CLAUDE.md`, `build-logic/API.md` exist.
- [ ] ADR: module layout (`core/<name>`, `feature/<name>`, features never depend on features) and the convention plugins.

## Out of scope
- Roborazzi (task 002), release signing/R8 (task 011), any UI beyond a single text.

## Notes
- Namespace/applicationId `app.pocketcp` (default — owner may change).
- The VM can't run Gradle: fetch wrapper files from the `gradle/gradle` repo at the release tag instead of `gradle wrapper`. Generated wrapper files don't count towards the line budget.

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
