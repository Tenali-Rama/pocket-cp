---
id: 016
title: Room cache database for contests and problems in core/database
milestone: M1
status: todo
deps: []
touches: [core/database/, settings.gradle.kts+, build.gradle.kts+, gradle/libs.versions.toml]
spec: ["4. Platform & Architecture", "14. Offline"]
---

## Goal
`core/database` provides the app's Room database (version 1). It holds the cache tables for contests and problems, plus a `cache_meta` table that records when each dataset was last fetched. The stale-while-revalidate layer (017) builds on it.

## Acceptance criteria
- [ ] Entities:
  - `ContestEntity`: id, name, phase, startEpochSeconds, durationSeconds, isGym
  - `ProblemEntity`: (contestId, index) key, name, rating, tags, solvedCount
  - `CacheMetaEntity(key, fetchedAtEpochMillis)`

  Field names mirror task 014's domain types. Mapping to the domain happens in 017, so this module doesn't depend on `core/model`.
- [ ] DAOs:
  - `Flow` queries: contests by gym flag, ordered by start time descending; problems ordered by contest id descending, then index
  - a `@Transaction replaceAll(...)` that swaps a whole dataset and writes its `cache_meta` row
  - `fetchedAt(key)`
- [ ] Room is configured in `core/database/build.gradle.kts` with KSP and the Room Gradle plugin. The schema is exported to `core/database/schemas/` and committed.
- [ ] Hilt `DatabaseModule` provides the database (singleton) and the DAOs.
- [ ] `ContestDaoTest` and `ProblemDaoTest` use an in-memory Robolectric database:
  - the `Flow` re-emits after `replaceAll`
  - `replaceAll` removes rows missing from the new set
  - tags round-trip, including an empty list
  - a problem with a `null` rating stays `null`
- [ ] `core/database/CLAUDE.md` explains the migration rule: any schema change bumps the version and adds a migration test. `core/database/API.md`.

## Out of scope
- Users, code files, notes and snippets tables (later milestones). Cache size limits and wipe (M13).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
