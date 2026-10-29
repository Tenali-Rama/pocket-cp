---
id: 014
title: Judge-agnostic domain model and JudgeSource in core/model
milestone: M1
status: todo
deps: []
touches: [core/model/, settings.gradle.kts+, gradle/libs.versions.toml, docs/decisions/]
spec: ["4. Platform & Architecture", "15. Settings"]
---

## Goal
A pure Kotlin/JVM module, `core/model`, holds the judge-agnostic domain that the UI and cache use: `Contest`, `Problem`, `User`, `JudgeError` and the `JudgeSource` interface. It also holds `OptionalModule`, the stable ids of the optional modules that the user can switch off (spec §15).

## Acceptance criteria
- [ ] `core/model` applies `pocketcp.jvm.library` and has no Android or Codeforces dependency. Times use `java.time` (`Instant`, `Duration`).
- [ ] Types:
  - `JudgeId`
  - `ContestId` / `Contest`: name, `ContestPhase` (`Upcoming`, `Running`, `Finished`, `Unknown`), start, duration, `isGym`
  - `ProblemId(contestId, index)` / `Problem`: name, rating?, tags, solvedCount?
  - `User`: handle, rating?, maxRating?, avatarUrl?
  - `JudgeError`: `Offline`, `RateLimited`, `Unavailable`, `Rejected(message)`, `Malformed`
- [ ] `interface JudgeSource { val judge: JudgeId; suspend fun contests(gym: Boolean): List<Contest>; suspend fun problemset(): List<Problem>; suspend fun users(handles: List<String>): List<User> }`. Each method throws only `JudgeError`.
- [ ] `enum class OptionalModule(val key: String)` lists exactly the §15 modules: local compute, AI, stress tester, practice & streaks, stats, other-judges calendar, focus mode auto-start. Each `key` is a stable lowercase string.
- [ ] Tests:
  - `ProblemIdTest`: `ProblemId(4, "A")` displays as `4A`, and `null` contest ids are handled
  - `OptionalModuleTest`: keys are unique, and `fromKey` of an unknown key returns `null`
- [ ] An ADR records the judge-agnostic domain and `JudgeSource` as the only way the data layer reaches a judge.
- [ ] `core/model/CLAUDE.md` and `API.md`.

## Out of scope
- `Submission`: M6 adds it together with the first endpoint that returns one.
- The Codeforces mapping (015) and any persistence.

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
