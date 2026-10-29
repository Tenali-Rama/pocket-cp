# 0007 — Retrofit runs through the request queue

- **Status**: accepted
- **Context**: Task 008 wants a Retrofit service, but every call must go through the 007 `RequestQueue` (spacing, dedup, backoff), and `core/network/` must not change for Codeforces.
- **Decision**:
  - Retrofit's `callFactory` is `QueueCallFactory` (core/codeforces): each Retrofit call becomes `queue.execute(request, CallLimitClassifier)` in an app-lifetime scope. Only Retrofit's `suspend` methods (async `enqueue`) are supported.
  - Queue errors cross Retrofit wrapped in `QueueCallFailure` (an `IOException`) and `CodeforcesApi` unwraps them into `CodeforcesError`.
  - JSON is kotlinx.serialization with `ignoreUnknownKeys`.
- **Consequences**: New API methods are one annotated `suspend fun` and get the queue for free. Never give Retrofit an `OkHttpClient`, and never add blocking (`Call.execute`) service methods.
