# core/model

## Rules
- Pure Kotlin/JVM (`pocketcp.jvm.library`). No Android, Compose, Hilt or Codeforces dependency; times use `java.time`.
- Serialization: only `kotlinx-serialization-core` annotations (`@Serializable`, `@SerialName`), and only on domain documents the app persists as JSON (e.g. `Statement`). No `Json`, format or custom serializer in main; encoding belongs to the module that stores it (ADR 0017).
- Every `@SerialName` of a persisted type is a stored key: give each property and sealed subtype an explicit one, and never change or reuse it.
- Judge-agnostic: nothing here names an API field, endpoint or DTO. Mapping a judge's data to these types belongs to that judge's `JudgeSource` (ADR 0009).
- `JudgeSource` methods throw only `JudgeError`. Add a `JudgeError` case only when the UI shows a distinct state for it.
- `OptionalModule.key`, `StatementLanguage.key` and `JudgeId.value` are persisted. Never change or reuse one; add new entries instead.
- Types are added with the first task that needs them (e.g. `Submission` in M6), not ahead of time.
- Tests: plain JUnit, no fixtures needed (no parsing here). `kotlinx-serialization-json` is a test-only dependency for round trips.
