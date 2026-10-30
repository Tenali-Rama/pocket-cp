# core/codeforces API

Typed client for the public Codeforces API (`contest.list`, `problemset.problems`, `user.info`) on top of the shared `RequestQueue`, with `OK`/`FAILED` envelope handling.

Package `app.pocketcp.core.codeforces`. Hilt: `CodeforcesModule` (singleton) provides `CodeforcesApi` on the app's `RequestQueue`. It binds no `JudgeSource`: `app` binds `CodeforcesJudgeSource` per build type (ADR 0015).

## `CodeforcesApi(queue, scope, baseUrl = BASE_URL)`
Each method returns the envelope's `result` or throws `CodeforcesError`. Calls run in `scope` (app lifetime) through `queue`, so they are spaced 2 s apart, deduplicated and backed off.

| Method | API call |
|--------|----------|
| `suspend fun contests(gym: Boolean = false): List<ContestDto>` | `contest.list`, `gym=true` only for Gym |
| `suspend fun problemset(tags: List<String> = emptyList()): ProblemsetDto` | `problemset.problems?tags=a;b` (no `tags` when empty) |
| `suspend fun users(handles: List<String>): List<UserDto>` | `user.info?handles=a;b` (at least one handle) |

## `CodeforcesError`
| Case | When |
|------|------|
| `Api(comment)` | `status: FAILED` (Codeforces sends it with HTTP 400), e.g. handle not found |
| `RateLimited` | "Call limit exceeded" (HTTP 200 or 400; its classifier sees both) or HTTP 429, after the queue's retries (2/4/8 s) |
| `Unavailable(code)` | HTTP 502/503/504 after the queue's retries |
| `Offline(cause)` | no connection or timeout |
| `Http(code)` | any other status without a `FAILED` envelope |
| `Malformed(detail)` | a body that doesn't decode into the expected DTO |

## DTOs
`ContestDto`, `ProblemsetDto(problems, problemStatistics)`, `ProblemDto`, `ProblemStatisticsDto`, `UserDto`: the API's objects with its field names; optional fields are nullable, enum-like fields (`type`, `phase`) are strings.

## `judge.CodeforcesJudgeSource(api) : JudgeSource`
Codeforces as the app's `JudgeSource` (ADR 0009): `judge` is `CodeforcesJudgeSource.ID` = `JudgeId("codeforces")`. DTOs and `CodeforcesError` never leave it.

| Codeforces | Domain |
|------------|--------|
| `phase` `BEFORE` | `ContestPhase.Upcoming` |
| `phase` `CODING`, `PENDING_SYSTEM_TEST`, `SYSTEM_TEST` | `ContestPhase.Running` |
| `phase` `FINISHED` | `ContestPhase.Finished` |
| any other `phase` | `ContestPhase.Unknown` |
| `startTimeSeconds`, `durationSeconds` | `start: Instant?` (`null` when absent), `duration: Duration` |
| `contests(gym = true)` | every contest has `isGym = true` |
| `problemStatistics` | joined on `(contestId, index)` as `solvedCount`; `null` when a problem has none |
| problem without `rating` | `rating = null` (never 0) |
| problem without `contestId` (e.g. acmsguru) | dropped |
| unrated user | `rating`, `maxRating` `null`; `avatar` → `avatarUrl` |

| `CodeforcesError` | `JudgeError` (the original is its `cause`) |
|-------------------|------------|
| `Offline` | `Offline` |
| `RateLimited` | `RateLimited` |
| `Unavailable` | `Unavailable` |
| `Api(comment)` | `Rejected(comment)` |
| `Http`, `Malformed` | `Malformed` |
