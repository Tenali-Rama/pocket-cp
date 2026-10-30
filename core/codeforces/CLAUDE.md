# core/codeforces

## Rules
- The only place that knows Codeforces API endpoints, the `OK`/`FAILED` envelope and its DTOs. HTML parsing (M3) and authenticated methods (M4) are separate.
- Every call goes through the app's `RequestQueue`: Retrofit's `callFactory` is `QueueCallFactory`. Never give Retrofit an `OkHttpClient` or call OkHttp here (ADR 0007).
- "Call limit exceeded" retries are the queue's job, via `CallLimitClassifier`. Don't sleep or retry here, and don't edit `core/network/` for Codeforces needs.
- DTOs mirror the API's field names and decode with `ignoreUnknownKeys`. Enum-like fields stay strings; the domain layer (M1) maps them. Nothing outside the data layer should see a DTO.
- Errors leave as `CodeforcesError`; a queue `NetworkError` never escapes `CodeforcesApi`.
- Tests: `FixtureServer` + recorded fixtures in `testdata/codeforces/api/` (see `testdata/README.md`), a real `RequestQueue`, and virtual time (`withApi` in the tests). No live network.

## Live canary
- `./gradlew liveCanary` (nightly in `canary.yml`, which files an issue with the last 40 lines on failure) runs `src/canary/` against live Codeforces. It is the unit test source set (`testLive`) of a `live` build type whose variant exists only when `liveCanary` is on the command line, so `ciCheck` and the unit tests never compile it. The IDE doesn't see it either.
- Add a check as a `@Test` in `src/canary/kotlin/.../canary/` (package `app.pocketcp.core.codeforces.canary`, the only one `liveCanary` runs). Use the shared `LiveCodeforces` so every call goes through one `RequestQueue`:
  - API: `live.endpoint("<method>") { it.<call>() }`, then assert with a `CanaryReport.endpointBroken(...)` message.
  - HTML: `live.page(url, "<css selector>")`, which fails with `SELECTOR BROKEN: <selector> on <url>`.
- Every failure message must name what broke (`ENDPOINT BROKEN`, `SELECTOR BROKEN`, `PAGE BROKEN`); a Cloudflare challenge is `BLOCKED by Cloudflare: <url>`, never a selector failure. Messages are tested offline in `CanaryReportTest`.
