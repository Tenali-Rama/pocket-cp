# 0003 — Request queue policy

- **Status**: accepted
- **Context**: Codeforces allows one API call per 2 s and answers bursts with 429 or "Call limit exceeded"; it is also often down (502/503/504). Spec §4/§16 ask for a queue with dedup and backoff, and for "down" to stay distinct from "rate-limited".
- **Decision**:
  - All traffic goes through one `RequestQueue`. Paths under `/api/` start ≥ 2 s apart (FIFO mutex held 2 s from each start, retries included); HTML pages share dedup and cookies but are not spaced.
  - Concurrent GETs of one URL share a single call that runs in the queue's own scope, so a cancelled caller never cancels it.
  - 429 → `RateLimited`, 502/503/504 → `Unavailable`, and 2xx or 4xx (not 429) responses a caller's `RetryClassifier` marks retryable all retry after 2, 4, 8 s, then fail (Codeforces sends "Call limit exceeded" with HTTP 400). `IOException`/timeouts fail at once as `Offline`. Other statuses go back to the caller.
- **Consequences**: Callers never sleep or retry themselves. Endpoint-specific errors plug in via `RetryClassifier`, not by changing the queue. Don't shorten the 2 s spacing or add a second client.
