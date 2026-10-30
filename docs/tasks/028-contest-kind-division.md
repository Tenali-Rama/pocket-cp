---
id: 028
title: Contest kind and division in the domain and cache; refresh survives Room write errors
milestone: M2
status: todo
deps: []
touches: [core/model/src/main/kotlin/app/pocketcp/core/model/Contest.kt, core/model/src/main/kotlin/app/pocketcp/core/model/JudgeError.kt, core/model/src/test/kotlin/app/pocketcp/core/model/ContestDivisionTest.kt, core/model/API.md, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesMapping.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesJudgeSourceTest.kt, core/database/src/main/kotlin/app/pocketcp/core/database/Entities.kt, core/database/src/main/kotlin/app/pocketcp/core/database/PocketCpDatabase.kt, core/database/src/main/kotlin/app/pocketcp/core/database/DatabaseModule.kt, core/database/src/test/, core/database/schemas/, core/database/build.gradle.kts, core/database/API.md, core/data/src/main/kotlin/app/pocketcp/core/data/Mappers.kt, core/data/src/main/kotlin/app/pocketcp/core/data/CachedResource.kt, core/data/src/test/kotlin/app/pocketcp/core/data/MappersTest.kt, core/data/src/test/kotlin/app/pocketcp/core/data/StaleWhileRevalidateTest.kt, core/data/API.md, gradle/libs.versions.toml]
spec: ["7.2 Contests", "7.3 Problemset", "4. Platform & Architecture"]
---

## Goal
Contests can be filtered by division and type (spec §7.2, and the problemset's division filter in §7.3), so the domain needs both. This task also folds in two M1 follow-ups:
- a Room write error during a refresh no longer escapes the refresh scope and crashes the app (review on #26)
- the `JudgeError.Unavailable` KDoc matches the mapping

## Acceptance criteria
- [ ] `Contest.kind: ContestKind` (`Cf | Icpc | Ioi | Unknown`), mapped from the API `type` (`CF`/`ICPC`/`IOI`; anything else → `Unknown`). Covered in `CodeforcesJudgeSourceTest` with `contest.list.json`.
- [ ] `Contest.division: ContestDivision` is derived from the name, not stored. Values: `Div1 | Div2 | Div3 | Div4 | Div1And2 | Educational | Global | Other`. Educational and Global take precedence over a "(Rated for Div. 2)" suffix (default — owner may change). `ContestDivisionTest` covers one real name per value, plus "Div. 1 + Div. 2" and a gym name → `Other`.
- [ ] Room goes to version 2: `contests.kind TEXT NOT NULL DEFAULT 'Unknown'`, with a `Migration(1, 2)` and the committed `2.json` schema. `MigrationTest` (`MigrationTestHelper`) migrates a v1 row and reads it back with `kind = Unknown`. `DatabaseModule` registers the migration.
- [ ] `core/data` mappers round-trip `kind`, and `MappersTest` covers it.
- [ ] `CachedResource.run()` also catches non-`JudgeError` exceptions from `fetch()` (e.g. `SQLiteException` from the Room write) and rethrows `CancellationException`. The refresh ends with `refreshing = false`, the cached `data` is unchanged, and `error = JudgeError.Unavailable(cause)` (default — owner may change). `StaleWhileRevalidateTest` adds this with a dataset whose write throws `SQLiteException`, and the test scope sees no uncaught exception.
- [ ] `JudgeError.Unavailable` KDoc reads "The judge is down or temporarily unreachable (e.g. HTTP 503); other unexpected statuses are `Malformed`."; `core/model/API.md` matches.

## Out of scope
- Filters UI (030, 033). Gym-specific fields (M8).

## Notes for next
<!-- builder fills in -->
