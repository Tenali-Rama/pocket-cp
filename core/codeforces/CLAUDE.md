# core/codeforces

## Rules
- The only place that knows Codeforces API endpoints, the `OK`/`FAILED` envelope and its DTOs. HTML parsing (M3) and authenticated methods (M4) are separate.
- Every call goes through the app's `RequestQueue`: Retrofit's `callFactory` is `QueueCallFactory`. Never give Retrofit an `OkHttpClient` or call OkHttp here (ADR 0004).
- "Call limit exceeded" retries are the queue's job, via `CallLimitClassifier`. Don't sleep or retry here, and don't edit `core/network/` for Codeforces needs.
- DTOs mirror the API's field names and decode with `ignoreUnknownKeys`. Enum-like fields stay strings; the domain layer (M1) maps them. Nothing outside the data layer should see a DTO.
- Errors leave as `CodeforcesError`; a queue `NetworkError` never escapes `CodeforcesApi`.
- Tests: `FixtureServer` + recorded fixtures in `testdata/codeforces/api/` (see `testdata/README.md`), a real `RequestQueue`, and virtual time (`withApi` in the tests). No live network.
