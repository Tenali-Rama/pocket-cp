---
id: 013
title: Retry Codeforces call-limit replies sent with HTTP 400
milestone: M1
status: done
deps: []
touches: [core/network/src/main/kotlin/app/pocketcp/core/network/RequestQueue.kt, core/network/src/main/kotlin/app/pocketcp/core/network/RetryClassifier.kt, core/network/src/test/kotlin/app/pocketcp/core/network/RequestQueueTest.kt, core/network/API.md, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CallLimitClassifier.kt, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesApi.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/CodeforcesQueueTest.kt, core/codeforces/API.md, docs/decisions/0003-request-queue-policy.md]
spec: ["4. Platform & Architecture", "16. Non-Functional"]
---

## Goal
Real Codeforces answers a burst with `status: FAILED`, comment "Call limit exceeded" and **HTTP 400**. Today the queue gives the `RetryClassifier` only 2xx bodies, so this reply fails at once. After this task it is retried with the 2/4/8 s backoff like a 2xx call-limit body. This is the open risk from task 008.

## Acceptance criteria
- [x] `RequestQueue` asks the classifier about 4xx responses (except 429, which keeps its own path) as well as 2xx. `Accept` on a 4xx still returns the response to the caller unchanged, so `RetryClassifier.AcceptAll` behaves exactly as before.
- [x] `RequestQueueTest` covers three cases on virtual time:
  - a 400 that the classifier marks `Retry` is retried after 2, 4 and 8 s and then throws its `finalError`
  - a 400 that succeeds on the second attempt returns the 2xx response
  - a 400 with `AcceptAll` is returned after one attempt
- [x] `CodeforcesQueueTest` serves `error.call-limit.json` with HTTP 400:
  - always → 4 attempts in total, then `CodeforcesError.RateLimited`
  - once, then a 200 `contest.list` fixture → returns the contests
  - an HTTP 400 `FAILED` "handle not found" → `CodeforcesError.Api` after exactly 1 attempt
- [x] ADR 0003's retry bullet and the tables in `core/network/API.md` and `core/codeforces/API.md` say that classifiers also see 4xx.

## Out of scope
- Any other change to the queue policy: spacing, dedup, the 429 and 5xx handling.

## Notes for next
- `RetryClassifier`s now see 2xx and 4xx (not 429). A classifier must `Accept` any 4xx it doesn't recognise, so callers still get it unchanged.
- `CallLimitClassifier` needed no code change: it keys on the body, so a 400 "Call limit exceeded" retries and a 400 "handle not found" is accepted and decoded as `CodeforcesError.Api`.
- `CodeforcesApi.kt` was not changed; it already maps the exhausted retry (`NetworkError.Rejected`) to `RateLimited`.
- The old "classifier only sees 2xx" test now uses a 500 (not in the retry set) to keep covering the pass-through path.
