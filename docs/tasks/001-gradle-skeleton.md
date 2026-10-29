---
id: 001
title: Gradle project, convention plugins, app module and ciCheck
milestone: M0
status: done
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
- [ ] The Gradle ktlint plugin is pinned to ktlint engine 1.3.1, the version of the CLI that `.github/workflows/ci.yml` installs; `build-logic/CLAUDE.md` says to bump both in the same PR.
- [ ] Gradle wrapper committed (`gradle/wrapper/`, `gradlew`, `gradlew.bat`), executable bit set on `gradlew`.
- [ ] `build-logic/` is an included build (`build-logic/settings.gradle.kts`, plugins in `build-logic/convention/`). Its docs live at the included-build root: `build-logic/CLAUDE.md` + `build-logic/API.md`. `app/CLAUDE.md` + `app/API.md` exist. (Task 012 checks exactly these roots: every `include(...)` in root `settings.gradle.kts`, plus `build-logic/`.)
- [ ] ADR: module layout (`core/<name>`, `feature/<name>`, features never depend on features) and the convention plugins.

## Out of scope
- Roborazzi (task 002), release signing/R8 (task 011), any UI beyond a single text.

## Notes
- Namespace/applicationId `app.pocketcp` (default — owner may change).
- The VM can't run Gradle: fetch wrapper files from the `gradle/gradle` repo at the release tag instead of `gradle wrapper`. Generated wrapper files don't count towards the line budget.

## Notes for next
- Versions: Gradle 8.14.3, AGP 8.10.1, Kotlin 2.1.21, KSP 2.1.21-2.0.1, Hilt 2.56.2, detekt 1.23.8, Compose BOM 2025.05.01; compile/targetSdk 36.
- Robolectric runs on SDK 34 via `app/src/test/resources/robolectric.properties` (SDK 35+ needs JDK 21). New Robolectric modules (e.g. 002) need the same pin or a JDK 21 test toolchain.
- `ciCheck` is per module (registered in `build-logic/.../Quality.kt`); extend with `tasks.named("ciCheck") { dependsOn(...) }` (011: assembleRelease).
- `pocketcp.hilt` adds only `hilt-android` + `hilt-compiler`; add `hilt-android-testing`/`kspTest` when a task first needs `@HiltAndroidTest`.
- Plugin ids in `build-logic/convention/build.gradle.kts` are string literals (ktlint 1.3.1 chain rule rejects `libs.plugins.x.get().pluginId`).
