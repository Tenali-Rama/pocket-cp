# 0002 — Module layout and convention plugins

- **Status**: accepted
- **Context**: Many builders add modules in parallel; per-module Gradle config would drift and each module would need its own CI wiring.
- **Decision**:
  - Shared code lives in `core/<name>/`, each feature in `feature/<name>/`, plus the thin `app` module. Features depend on `core`, never on another feature.
  - Every module applies only `pocketcp.*` convention plugins from the `build-logic/` included build (see `build-logic/API.md`); versions live in `gradle/libs.versions.toml`.
  - The base plugins register a per-module `ciCheck` (ktlint, detekt with type resolution, Android lint, unit tests, assemble); `./gradlew ciCheck` is the CI gate.
- **Consequences**: A new module joins CI by applying a convention plugin. Changing SDK levels, JDK or a quality gate is one edit in `build-logic/`. Don't configure AGP/Kotlin/Hilt in a module's own build file, and don't bypass `ciCheck`.
