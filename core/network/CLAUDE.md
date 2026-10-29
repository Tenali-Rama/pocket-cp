# core/network

## Rules
- Every request goes through `RequestQueue` and the single `OkHttpClient` from `NetworkModule`. Never build another client or call OkHttp directly from a data layer; that bypasses the rate limit, dedup and the shared cookie jar.
- Queue policy (spacing, dedup, backoff, error mapping) is ADR 0003. Change the ADR with the code.
- HTTPS only: the client has no cleartext `ConnectionSpec`, and the queue `require`s `https://`.
- Codeforces specifics (endpoints, DTOs, "Call limit exceeded" handling) don't belong here; they plug in through `RetryClassifier` from the data layer.
- Never log request bodies, cookies or headers (spec §16: no logging of credentials or code).
- Tests: queue logic on virtual time with a fake `HttpTransport` (`runTest`); OkHttp behaviour with `MockWebServer` over HTTPS (`okhttp-tls`). No live network.
