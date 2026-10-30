# core/network API

The single OkHttp client (HTTP/2, gzip, HTTP cache, HTTPS only), the shared cookie jar and the rate-limited `RequestQueue` every network call goes through.

Package `app.pocketcp.core.network`. Hilt: `NetworkModule` (singletons) provides `OkHttpClient`, `SharedCookieJar`, `CookieStore` and `RequestQueue`.

## `RequestQueue`
`suspend fun execute(request: Request, classifier: RetryClassifier = RetryClassifier.AcceptAll): NetworkResponse`
- HTTPS only (`IllegalArgumentException` otherwise).
- URLs whose path starts with `/api/` start ≥ `API_INTERVAL` (2 s) apart, FIFO. Other requests are not spaced.
- Concurrent GETs of the same URL share one upstream call; cancelling one caller doesn't cancel it.
- Throws `NetworkError`:

| Case | Retries (`BACKOFF` 2/4/8 s) | Error |
|------|-----------------------------|-------|
| HTTP 429 | yes | `RateLimited` |
| HTTP 502/503/504 | yes | `Unavailable(code)` |
| `IOException` / timeout | no | `Offline(cause)` |
| 2xx or 4xx (not 429), classifier says `Retry(finalError)` | yes | `finalError` |
| 2xx or 4xx (not 429), classifier says `Fail(error)` | no | `error` |

A 2xx or 4xx the classifier `Accept`s, and any other status, is returned unchanged as a `NetworkResponse` for the caller to interpret.

## Types
- `NetworkResponse(code, headers, body: String)`: fully read, shareable between deduplicated callers.
- `fun interface RetryClassifier { fun classify(response: NetworkResponse): RetryDecision }`, runs on 2xx and 4xx except 429.
- `RetryDecision`: `Accept | Retry(finalError) | Fail(error)`.
- `NetworkError`: `RateLimited | Unavailable(code) | Offline(cause) | Rejected(reason)`. `Rejected` is for classifiers.
- `HttpTransport` (one exchange, throws `IOException`); `OkHttpTransport(callFactory)` is the production one.
- `CookieStore { all(); put(cookies) }`, `InMemoryCookieStore`; `SharedCookieJar(store)` sends a cookie only where its domain/path/secure rules match and until it expires.
- `HttpClients.create(cache, cookieJar)`: the client configuration; `HttpClients.CACHE_SIZE_BYTES`.
