---
id: 037
title: Bounded running + next N upcoming contests query and repository API
milestone: M2
status: done
deps: []
touches: [core/database/src/main/kotlin/app/pocketcp/core/database/ContestDao.kt, core/database/src/test/kotlin/app/pocketcp/core/database/ContestDaoTest.kt, core/database/API.md, core/data/src/main/kotlin/app/pocketcp/core/data/ContestRepository.kt, core/data/src/main/kotlin/app/pocketcp/core/data/CachedResource.kt, core/data/src/test/kotlin/app/pocketcp/core/data/ActiveContestsTest.kt, core/data/src/test/kotlin/app/pocketcp/core/data/CacheWarmerTest.kt, feature/contests/src/test/kotlin/app/pocketcp/feature/contests/FakeContestRepository.kt, core/data/API.md]
spec: ["7.1 Home", "3. Performance Budgets"]
---

## Goal
Home needs only the running contests and the next few upcoming ones, but today the only source is `ContestRepository.contests(gym = false)`: every regular contest (about 2000 rows in live builds, ADR 0015), re-read on every cache write. After this task, `ContestRepository.activeContests(upcomingLimit)` returns just those rows from a bounded Room query, with the same stale-while-revalidate state as the regular dataset. PR #47's review found Home's warm-start memory 23–38 MB over main because it holds the full list.

## Acceptance criteria
- [ ] `ContestDao.activeContests(upcomingLimit: Int): Flow<List<ContestEntity>>` is one `@Query` over the regular dataset (`isGym = 0`) only. It returns:
  - every contest with phase `Running`
  - then at most `upcomingLimit` contests with phase `Upcoming`, earliest `startEpochSeconds` first, null starts last, ties by `id` ascending

  `Running` rows are ordered earliest start first, ties by `id` ascending. `Finished` and `Unknown` rows are never returned. No schema change and no new index: the database stays at version 2 (default — owner may change).
- [ ] `ContestDaoTest` covers:
  - with 2000 finished, 2 running and 8 upcoming regular rows plus running/upcoming gym rows, `activeContests(5)` returns exactly the 2 running and the 5 earliest upcoming, in order
  - an upcoming contest without a start comes after the dated ones
  - `activeContests(0)` returns only the running rows
  - the flow re-emits after `replaceAll(gym = false, ...)`
- [ ] `ContestRepository.activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>>` (regular contests only):
  - it is a view of the same `CachedResource` as `contests(gym = false)`. It shares `fetchedAt`, `refreshing`, `error`, staleness (`MAX_AGE`) and the single in-flight refresh. `refresh(gym = false, ...)` refreshes both views. `CachedResource` gets a stream over another `Flow` of the same dataset; it does not get a second policy (core/data rule).
  - `data` is `null` until the first fetch, and the bounded list after that, possibly empty
  - `IllegalArgumentException` if `upcomingLimit < 0`
  - the interface method has no default body, so no implementation can silently fall back to the full `contests(false)` list
- [ ] The two existing test fakes implement the new method, and nothing else in them changes:
  - `CacheWarmerTest.FakeContestRepository.activeContests` returns `emptyFlow()`, like its `contests`
  - `feature/contests` `FakeContestRepository.activeContests` fails the test if called (`error("The Contests tab never uses the bounded view")`)
- [ ] `ActiveContestsTest` (in-memory Room, `FakeJudgeSource`, `SchedulerClock`, Turbine, as in `StaleWhileRevalidateTest`) covers:
  - the first item is `data = null, refreshing = true`; after the fetch, only running + N upcoming are emitted, in DAO order
  - after a refresh in which the earliest Upcoming contest comes back as Running, the next emission lists it among the running rows, and the next-earliest upcoming contest fills its upcoming slot (the zero-crossing case Home relies on)
  - `activeContests(-1)` throws `IllegalArgumentException`
  - collecting `activeContests(5)` and `contests(gym = false)` at once triggers one judge fetch
  - a failed refresh keeps the bounded data and sets `error`
  - a fresh cache (younger than `MAX_AGE`) causes no fetch
- [ ] `core/database/API.md` and `core/data/API.md` document the new functions and their ordering.

## Out of scope
- Any change in `feature/home` (034 switches to this API). Any change in `feature/contests` beyond the one stub in its test fake.
- Moving `Contest.asOf` to `core/model`. Making Upcoming lists time-aware in SQL (a `Flow` query with a fixed `now` goes stale). Indexes or a migration.
- Gym: there is no bounded gym view.

## Notes for next
- Home (034) collects `ContestRepository.activeContests(n)`. It shares the regular dataset's refresh, so `refresh(gym = false)` and pull-to-refresh need no new call.
- `CachedResource.stream(cached: Flow<R>)` is the hook for further bounded views of one dataset; it keeps one status and one in-flight refresh.
- Ordering uses the stored phase; Upcoming → Running by the clock alone is not in SQL and waits for a refresh (scoped out here).
- SQLite treats a negative `LIMIT` as no limit, so the repository rejects `upcomingLimit < 0` before building the flow.
