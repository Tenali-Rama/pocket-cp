---
id: 008
title: Codeforces API client and fixture test harness
milestone: M0
status: done
deps: [007]
touches: [core/codeforces/, core/testing/, testdata/README.md, testdata/codeforces/api/, settings.gradle.kts+, gradle/libs.versions.toml]
spec: ["4. Platform & Architecture"]
---

## Goal
A typed Codeforces API client on top of the request queue, and a shared harness that serves recorded fixtures from `testdata/` so no test touches the network.

## Acceptance criteria
- [x] `core/testing/`: `Fixtures.text(path)` reads from the repo-root `testdata/`, and `FixtureServer` maps request paths to fixtures. `FixtureServerTest`: a mapped path returns the fixture, and an unmapped path fails the test with the path in the message.
- [x] `core/codeforces/`: Retrofit service with kotlinx.serialization for `contest.list` (incl. `gym`), `problemset.problems` (tags) and `user.info` (handles); every call goes through the 007 queue.
- [x] Envelope handling: `status: OK` → result; `status: FAILED` → `CodeforcesError.Api(comment)`; a "Call limit exceeded" comment is retried with the queue's backoff and then becomes `RateLimited`. This uses a `RetryClassifier` passed to the 007 queue; no edits to `core/network/`.
- [x] `CodeforcesApiTest` against recorded fixtures in `testdata/codeforces/api/` (trimmed real responses: `contest.list.json`, `contest.list.gym.json` (`gym=true`), `problemset.problems.json`, `user.info.json`, `error.call-limit.json`, `error.handle-not-found.json`): decodes all fields used, ignores unknown fields, sends `gym=true` for the gym list, and maps both errors; passes in CI.
- [x] `CodeforcesQueueTest` (virtual time): three different client calls start at t = 0 s, 2 s and 4 s, which proves they go through the queue.
- [x] `testdata/README.md`: how to record and trim a fixture (no personal data, no cookies).
- [x] `CLAUDE.md` + `API.md` for `core/codeforces/` and `core/testing/`.

## Out of scope
- Repositories, Room cache, domain models (M1). Authenticated API methods (M4). HTML parsing (M3).

## Notes
- JSON via kotlinx.serialization (default — owner may change).

## Notes for next
- Retrofit goes through the queue via `QueueCallFactory` (Retrofit `callFactory`, ADR 0004). New API methods: add a `suspend fun` to `CodeforcesService` and a wrapper in `CodeforcesApi`; never give Retrofit an `OkHttpClient`.
- Real Codeforces sends `FAILED` with HTTP 400; that path is mapped from the error body (`Api` / `RateLimited`, no retry). A 2xx "Call limit exceeded" is retried by the queue via `CallLimitClassifier`. If Codeforces sends the call limit with 503, the queue reports `Unavailable` (core/network owns that).
- DTOs keep API field names and string enums (`type`, `phase`); M1 maps them to the judge-agnostic domain. `UserDto` leaves out `city` on purpose (it's the unknown-field check in the test).
- Tests: `testImplementation(project(":core:testing"))`, then `FixtureServer().map(path, fixture, code)` + `withApi(...)` (core/codeforces tests) on virtual time. `error.call-limit.json` is hand-written (see `testdata/README.md`).
- Added `alias(libs.plugins.kotlin.serialization) apply false` to the root `build.gradle.kts` (plugin-on-classpath rule) and ADR 0004; both outside `touches`.
