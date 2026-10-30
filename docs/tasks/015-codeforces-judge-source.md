---
id: 015
title: CodeforcesJudgeSource mapping API DTOs to the domain
milestone: M1
status: done
deps: [014]
touches: [core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/judge/, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesModule.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/, core/codeforces/build.gradle.kts, core/codeforces/API.md, testdata/codeforces/api/]
spec: ["4. Platform & Architecture"]
---

## Goal
Codeforces is the first `JudgeSource`. `CodeforcesJudgeSource` wraps `CodeforcesApi`, maps its DTOs to `core/model` types and its errors to `JudgeError`. Hilt binds it as `JudgeSource`.

## Acceptance criteria
- [ ] `CodeforcesJudgeSourceTest` runs against the recorded fixtures through `FixtureServer`:
  - `contest.list`: the phases `BEFORE` → `Upcoming`, `CODING` and `PENDING_SYSTEM_TEST` / `SYSTEM_TEST` → `Running`, `FINISHED` → `Finished`; any other phase → `Unknown`
  - start and duration are converted from seconds
  - `gym=true` sets `isGym`
- [ ] `problemset` joins `problemStatistics` to the problems as `solvedCount`. A problem without a rating or statistics maps to `null`, never to 0.
- [ ] A problem whose `contestId` is absent is dropped, not mapped (default — owner may change). `CodeforcesJudgeSourceTest` uses a fixture with one such problem and asserts the result has one problem fewer than the fixture and none of them has that problem's name.
- [ ] `users`: an unrated user has `rating` and `maxRating` `null`.
- [ ] Error mapping test, one case per `CodeforcesError`:
  - `Offline` → `Offline`
  - `RateLimited` → `RateLimited`
  - `Unavailable` → `Unavailable`
  - `Api(comment)` → `Rejected(comment)`
  - `Http` / `Malformed` → `Malformed`
- [ ] `CodeforcesModule` binds `JudgeSource` to `CodeforcesJudgeSource` (singleton).
- [ ] `core/codeforces/API.md` documents the mapping.

## Out of scope
- Caching (016/017). New API endpoints.

## Notes for next
- Inject `JudgeSource` (bound in `CodeforcesModule`, singleton); never `CodeforcesApi` from the data layer. Its `judge` id `codeforces` is the cache key.
- Mapping lives in `core/codeforces/.../judge/CodeforcesMapping.kt` (internal); every `JudgeError` carries the original `CodeforcesError` as `cause`.
- `CodeforcesError.Http` maps to `JudgeError.Malformed` as the criteria ask, although `JudgeError.Unavailable`'s KDoc mentions "unexpected HTTP status"; align the KDoc if that matters.
- Problems without `contestId` are dropped (ADR 0009); `solvedCount`/`rating` stay `null`, never 0.
- New fixtures: `contest.list.phases.json` (hand-edited phases), `problemset.problems.mixed.json`, `user.info.unrated.json`; tests use `withSource` (test helper in `judge/CodeforcesJudgeSourceTest.kt`).
