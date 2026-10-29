# app

## Rules
- Keep this module thin: application class, `MainActivity`, top-level wiring. Screens and logic live in `feature/*` and `core/*`.
- Plugins come only from build-logic (`pocketcp.android.application`, `pocketcp.android.compose`, `pocketcp.hilt`).
- Tests are Robolectric on the JVM (`src/test/`) and launch the real `PocketCpApplication`, so they exercise the production Hilt graph. Robolectric runs on SDK 34 (`src/test/resources/robolectric.properties`): SDK 35+ needs JDK 21.
- `ciCheck` for this module is defined by the convention plugin; extend it with `tasks.named("ciCheck")` rather than redefining it. Here it also runs `assembleRelease`, so R8 breakage fails CI.
- Release builds use R8 minify and resource shrinking. A reflection or serialization keep rule goes in `proguard-rules.pro`, with a comment saying why.
- Signing: `-Pkeystore=<path>` plus env `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` signs the release APK (as `release.yml` does). Never commit a keystore or its passwords. See ADR 0003.
- Versioning: `appVersionName` / `appVersionCode` at the top of `build.gradle.kts` are the only place the version lives. Closing milestone n sets `versionName = "0.<n>.0"` and `versionCode = <n> + 1`, in the last PR of that milestone and before the `v0.<n>.0` tag is pushed. `versionCode` only ever goes up.
