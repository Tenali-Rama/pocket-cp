# build-logic

Included build (`pluginManagement { includeBuild("build-logic") }`) holding the convention plugins in `convention/`.

## Rules
- Modules never configure AGP, Kotlin, Compose, Hilt, ktlint or detekt directly; add or change a convention plugin here.
- Plugin dependencies are `compileOnly` here and put on the classpath by `apply false` in the root `build.gradle.kts`. A new third-party plugin needs both, plus a `libs.versions.toml` entry.
- ktlint: the Gradle plugin runs ktlint engine `ktlint` from `libs.versions.toml` (1.3.1). The CLI in `.github/workflows/ci.yml` (and on the VM) must be the same version. **Bump both in the same PR.**
- detekt: `config/detekt/detekt.yml` builds on the default config. Rules that need type resolution (e.g. `UnsafeCallOnNullableType`, the no-`!!` rule) only run in `detektMain`, which is what `ciCheck` uses. Don't switch `ciCheck` to plain `detekt`.
- Every new gate goes into `ciCheck` via `configureQuality`, so all modules pick it up.
- The VM can't run Gradle. CI compiles this build.
