---
id: 042
title: Room v3 statements cache table and StatementDao
milestone: M3
status: done
deps: []
touches: [core/database/src/main/kotlin/app/pocketcp/core/database/StatementEntity.kt, core/database/src/main/kotlin/app/pocketcp/core/database/StatementDao.kt, core/database/src/main/kotlin/app/pocketcp/core/database/PocketCpDatabase.kt, core/database/src/main/kotlin/app/pocketcp/core/database/DatabaseModule.kt, core/database/src/main/kotlin/app/pocketcp/core/database/Migrations.kt, core/database/src/test/kotlin/app/pocketcp/core/database/StatementDaoTest.kt, core/database/src/test/kotlin/app/pocketcp/core/database/MigrationTest.kt, core/database/src/test/kotlin/app/pocketcp/core/database/DatabaseModuleTest.kt, core/database/schemas/, core/database/API.md]
spec: ["14. Offline", "3. Performance Budgets"]
---

## Goal
Spec §14 wants viewed statements available offline, and §3 wants a statement open from cache in under 100 ms. After this task the Room database (version 3) has a `statements` table keyed by problem and language, holding the serialized statement document, so 043 can cache and prefetch statements.

## Acceptance criteria
- [ ] `StatementEntity` (`statements`), key `(contestId, index, language)`:
  - `language: String` (the `StatementLanguage` key; entities stay free of `core/model`)
  - `document: String` (JSON written by `core/data`)
  - `formatVersion: Int`
  - `fetchedAtEpochMillis: Long`
- [ ] `StatementDao` (provided by `DatabaseModule`):
  - `fun statement(contestId, index, language): Flow<StatementEntity?>`: re-emits after a write to that row
  - `suspend fun upsert(entity)`
  - `suspend fun cachedIndices(contestId: Int, language: String, formatVersion: Int): List<String>`: the indices with a current-format row, for prefetch
- [ ] `PocketCpDatabase` goes to version 3. `Migrations.kt` adds `2 → 3` (creates the table and keeps every cached contest, problem and `cache_meta` row), and `DatabaseModule` registers it. The exported `schemas/…/3.json` is committed; `1.json` and `2.json` are untouched.
- [ ] `MigrationTest`: from the committed version 2 schema with contest, problem and meta rows, `2 → 3` keeps all of them and the new table works. `1 → 2 → 3` also succeeds.
- [ ] `StatementDaoTest` (in-memory, Turbine):
  - `statement` emits `null`, then the row after `upsert`, then the replacement after a second `upsert`
  - `en` and `ru` rows are separate
  - `cachedIndices` ignores other contests, other languages and rows with another `formatVersion`
- [ ] `core/database/API.md` documents the table, the DAO and the migration.

## Out of scope
- JSON mapping, staleness and prefetch (043). Cache eviction or size limits: the Data settings are M13, and there's no eviction in M3 (default — owner may change).
- Tables for bookmarks, notes, tags or recent problems (M10 / M4).

## Notes for next
- `schemas/…/3.json` was written without a local build: it is 2.json plus the `statements` entity, with the identity hash computed by Room's algorithm (checked to reproduce the 1.json and 2.json hashes). CI's build regenerates it; a diff there would mean a field/order mismatch.
- `StatementDao.statement` is a Room `Flow`, so it re-emits on any write to `statements`, not only to its own row; 043 can add `distinctUntilChanged()` if that matters.
- No extra index: the primary key `(contestId, index, language)` already serves `cachedIndices`' `contestId` filter.
- `core/database/CLAUDE.md` now allows per-row `upsert` for per-item caches like `statements` (outside `touches`, a one-line rule fix).
- Migrations are listed one by one in `addMigrations` (detekt `SpreadOperator`); the next schema change adds `MIGRATION_3_4` there and in `MigrationTest`.
