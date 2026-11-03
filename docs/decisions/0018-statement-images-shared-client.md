# 0018 — Statement images use the shared client outside the request queue

- **Status**: accepted
- **Context**: Statement images are binary bodies and need offline disk caching, while the API queue spaces and deduplicates judge requests.
- **Decision**: Coil's statement `ImageLoader` uses the singleton network `OkHttpClient` and its own disk cache; image fetches do not use `RequestQueue`.
- **Consequences**: Images share cookies, HTTPS-only transport and HTTP configuration, but are not subject to API spacing. Keep the loader factory on the shared client.
