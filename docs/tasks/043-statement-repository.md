---
id: 043
title: StatementRepository with stale-while-revalidate and contest prefetch
milestone: M3
status: todo
deps: [041, 042]
touches: [core/data/src/main/kotlin/app/pocketcp/core/data/StatementRepository.kt, core/data/src/main/kotlin/app/pocketcp/core/data/CachedResource.kt, core/data/src/main/kotlin/app/pocketcp/core/data/Mappers.kt, core/data/src/main/kotlin/app/pocketcp/core/data/DataModule.kt, core/data/src/test/kotlin/app/pocketcp/core/data/StatementRepositoryTest.kt, core/data/src/test/kotlin/app/pocketcp/core/data/MappersTest.kt, core/data/src/test/kotlin/app/pocketcp/core/data/TestHarness.kt, core/data/build.gradle.kts, core/data/API.md, core/data/CLAUDE.md, app/src/test/kotlin/app/pocketcp/data/DataWiringTest.kt, gradle/libs.versions.toml, docs/decisions/0011-stale-while-revalidate.md]
spec: ["3. Performance Budgets", "14. Offline", "7.4 Problem View"]
---

## Goal
After this task features get statements cache first: `StatementRepository.statement(problem, language)` emits the cached statement at once and refreshes it when missing or stale. Contest screens can prefetch every statement of a contest (spec §3), and a statement once viewed works offline (spec §14).

## Acceptance criteria
- [ ] `StatementRepository` is a public interface with an `internal` implementation (`CachedStatementRepository`, in the same file, as `CachedContestRepository` is), bound as a singleton in `DataModule`, the way `ContestRepository` and `ProblemRepository` are. Features and `core/problemview` see only the interface, so their tests use fakes (046, 049) without touching core/data. The interface has:
  - `fun statement(problem, language): Flow<Cached<Statement>>`, with the ADR 0011 policy: cache first; refresh when missing, older than `MAX_AGE` (7 days, default — owner may change), in the future, or stored with another `formatVersion` (treated as missing); one in-flight refresh per `(problem, language)`, shared by collectors and `refresh`; a failure sets `error` and never clears `data`
  - `suspend fun refresh(problem, language, force = false)`
  - `fun prefetch(problems: List<ProblemId>, language)`: in the repository's app-lifetime scope, fetches one at a time those without a current-format row (`StatementDao.cachedIndices`); stops at the first `JudgeError.Offline`, skips other failures; never throws to the caller
  - `fun pageUrl(problem, language): String`, which is `JudgeSource.problemUrl`
- [ ] The policy reuses `CachedResource`, extended for keyed rows if needed. There is no second stale-while-revalidate implementation (core/data rule). Contest and problemset behaviour doesn't change: `StaleWhileRevalidateTest` and `ActiveContestsTest` pass unchanged.
- [ ] `Mappers.kt` maps `Statement` ↔ `StatementEntity` through `Json` with `STATEMENT_FORMAT_VERSION = 1`. `MappersTest` round-trips a statement with every block and inline type.
- [ ] `StatementRepositoryTest` (in-memory Room, `FakeJudgeSource`, `SchedulerClock`, Turbine) covers:
  - the first item is `data = null, refreshing = true`, and the fetched statement follows
  - a cached, fresh statement is emitted first with no judge call (the < 100 ms open path)
  - a stale statement emits cached data, then refreshes
  - a failed refresh keeps the data and sets `error`
  - a row with another `formatVersion` is refetched
  - two collectors cause one fetch
  - `prefetch` of 3 problems with 1 cached makes 2 sequential calls; `Offline` on the first stops the rest; a `Malformed` one is skipped and the next is fetched
  - `prefetch` while a collector already fetches the same statement shares that fetch
- [ ] `DataWiringTest` injects `StatementRepository` and gets the same instance twice. `core/data/API.md` documents that it is an interface and that tests fake it.
- [ ] ADR 0011 gets the statements' max age.

## Out of scope
- Prefetching images (Coil's disk cache holds images of viewed statements, 044). Prefetch when a contest starts in the background: Codeforces' problemset doesn't list a running contest's problems, so it waits for M8's standings, where ROADMAP's M8 line records it (default — owner may change).
- Any UI.

## Notes for next
<!-- builder fills in -->
