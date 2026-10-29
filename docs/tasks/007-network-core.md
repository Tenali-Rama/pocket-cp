---
id: 007
title: Network core with rate-limited request queue and shared cookie jar
milestone: M0
status: todo
deps: [001]
touches: [core/network/, settings.gradle.kts+, gradle/libs.versions.toml, docs/decisions/]
spec: ["4. Platform & Architecture", "16. Non-Functional"]
---

## Goal
`core/network/` provides the single OkHttp client (HTTP/2, gzip, HTTP cache for ETag/If-Modified-Since), one shared cookie jar and a request queue that keeps Codeforces API calls under 1 request / 2 s.

## Acceptance criteria
- [ ] `RequestQueueTest` (virtual time, passes in CI):
  - three queued API calls start at t = 0 s, 2 s, 4 s;
  - two concurrent identical GETs make one upstream call and both callers get the result;
  - cancelling one waiter doesn't cancel the shared call for the other;
  - HTTP 429/503 retries after 2 s, 4 s, 8 s, then fails with a typed `NetworkError.RateLimited`.
- [ ] `SharedCookieJarTest`: a cookie set by a `codeforces.com` response is sent on the next request to that host and never to another host. Storage sits behind a `CookieStore` interface (in-memory here; M4 backs it with encrypted storage and syncs the WebView).
- [ ] `HttpClientTest` (MockWebServer): a 304 for a request with `If-None-Match` is served from the OkHttp cache; plain `http://` requests are rejected (HTTPS only).
- [ ] Hilt `NetworkModule` provides the client, cookie jar and queue as singletons.
- [ ] `core/network/CLAUDE.md` + `API.md`; ADR on the queue policy.

## Out of scope
- Codeforces endpoints and DTOs, fixture harness (task 008). WebView cookie sync and encrypted storage (M4).

## Notes
- The queue throttles `/api/` calls only. HTML page fetches share the cookie jar and deduplication, without the 2 s spacing (default — owner may change).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
