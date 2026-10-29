---
id: 015
title: CodeforcesJudgeSource mapping API DTOs to the domain
milestone: M1
status: todo
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
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
