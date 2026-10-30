# 0009 — Judge-agnostic domain; `JudgeSource` is the only way to a judge

- **Status**: accepted
- **Context**: Spec §4 wants other judges addable later without reworking the UI, but the API client (`core/codeforces`) exposes Codeforces DTOs and `CodeforcesError`.
- **Decision**:
  - `core/model` (pure JVM) holds the domain types (`Contest`, `Problem`, `User`, ids) and `JudgeSource`. UI, ViewModels and the Room cache use only these.
  - The data layer reaches a judge only through a `JudgeSource`. Each judge's implementation maps its DTOs and its errors to the domain; its methods throw only `JudgeError`.
  - A judge record that the domain can't represent (e.g. a problem without a contest id) is dropped in the mapping, not modelled with nullable ids.
- **Consequences**: Adding a judge is a new `JudgeSource`, not a UI change. DTOs and judge-specific errors must never leave the data layer. New domain fields must make sense for any judge or be nullable.
