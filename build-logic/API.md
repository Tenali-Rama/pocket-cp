# build-logic API

Gradle convention plugins that every module applies instead of configuring AGP, Kotlin, Compose, Hilt and quality checks itself.

Apply them by alias from `gradle/libs.versions.toml`, e.g. `alias(libs.plugins.pocketcp.android.feature)`.

| Plugin id | Use for | What it sets up |
|-----------|---------|-----------------|
| `pocketcp.android.application` | `app` | AGP app + Kotlin, minSdk 26, compile/targetSdk 36, Java 17, quality + `ciCheck` |
| `pocketcp.android.library` | Android `core/<name>` | AGP library + Kotlin, same SDK/JDK, quality + `ciCheck` |
| `pocketcp.android.compose` | any module with UI (add to app/library) | Compose compiler, Compose BOM, Material 3, ui-tooling, `ui-test-junit4` for tests; Roborazzi screenshots (below) |
| `pocketcp.hilt` | any Android module with DI | KSP, Hilt plugin, `hilt-android` + `hilt-compiler` |
| `pocketcp.android.feature` | `feature/<name>` | `android.library` + `android.compose` + `hilt` |
| `pocketcp.jvm.library` | pure Kotlin `core/<name>` | Kotlin/JVM, Java 17, Android lint, JUnit, quality + `ciCheck` |

## `ciCheck`
Each module that applies `android.application`, `android.library` or `jvm.library` gets a `ciCheck` task:
- Android: `ktlintCheck`, `detektMain`, `lintDebug`, `testDebugUnitTest`, `assembleDebug`.
- JVM: `ktlintCheck`, `detektMain`, `lint`, `test`, `assemble`.
- Compose modules also: `verifyRoborazziDebug` (fails on a pixel diff or a missing golden).

`./gradlew ciCheck` (run by `scripts/verify.sh --full`) runs it in every module. A module adds extra gates with
`tasks.named("ciCheck") { dependsOn("…") }` in its own `build.gradle.kts`.

## Screenshots (`pocketcp.android.compose`)
- Applies the Roborazzi plugin with goldens in `<module>/src/test/screenshots/` (ADR 0005).
- Every unit test runs with Robolectric native graphics (`robolectric.graphicsMode=NATIVE`, hardware pixel copy).
- Adds `testImplementation(project(":core:screenshot"))` (except in that module): see `core/screenshot/API.md`.

## Constants
`MIN_SDK`, `COMPILE_SDK`, `TARGET_SDK` live in `convention/src/main/kotlin/app/pocketcp/buildlogic/KotlinAndroid.kt`.
