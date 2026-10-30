# 0010 — Stale-while-revalidate repositories; errors never clear data

- **Status**: accepted
- **Context**: Spec §2 "Instant" and §14 want cached data shown at once and kept offline, while the Codeforces API allows 1 request / 2 s.
- **Decision**:
  - `core/data` repositories emit `Cached<T>` from Room first, and refresh through `JudgeSource` when `fetchedAt` is missing, in the future, or older than the max age: contests 5 min, problemset 1 h (owner may change). `refresh(force = true)` ignores age.
  - One refresh per dataset at a time, in an app-lifetime scope; every collector shares it.
  - A failed refresh only sets `error`. It never clears or replaces the cached `data`; the next success clears `error`.
- **Consequences**: Screens never show a spinner over cached data and need no own caching. Max ages change here and in the repositories together. Don't add a "clear cache on error" path or per-collector fetches.
