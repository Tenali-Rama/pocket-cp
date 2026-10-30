# core/model

## Rules
- Pure Kotlin/JVM (`pocketcp.jvm.library`). No Android, Compose, Hilt, serialization or Codeforces dependency; times use `java.time`.
- Judge-agnostic: nothing here names an API field, endpoint or DTO. Mapping a judge's data to these types belongs to that judge's `JudgeSource` (ADR 0009).
- `JudgeSource` methods throw only `JudgeError`. Add a `JudgeError` case only when the UI shows a distinct state for it.
- `OptionalModule.key` and `JudgeId.value` are persisted. Never change or reuse one; add new entries instead.
- Types are added with the first task that needs them (e.g. `Submission` in M6), not ahead of time.
- Tests: plain JUnit, no fixtures needed (no parsing here).
