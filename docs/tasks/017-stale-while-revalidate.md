---
id: 017
title: Stale-while-revalidate repositories in core/data
milestone: M1
status: done
deps: [015, 016]
touches: [core/data/, settings.gradle.kts+, app/build.gradle.kts+, gradle/libs.versions.toml, docs/decisions/]
spec: ["2. Product Principles", "4. Platform & Architecture", "14. Offline"]
---

## Goal
`core/data` gives features cache-first data (spec §2 "Instant"). `ContestRepository` and `ProblemRepository` emit the Room cache at once, refresh it through `JudgeSource` in the background when it is stale, and keep the cached data when a refresh fails.

## Acceptance criteria
- [ ] A generic `Cached<T>(data: T?, fetchedAt: Instant?, refreshing: Boolean, error: JudgeError?)` type is exposed as a `Flow`.
- [ ] `ContestRepository.contests(gym)` and `ProblemRepository.problemset()` follow this policy:
  - emit the cache first
  - refresh when there is no `fetchedAt`, or when it is older than the max age: contests 5 min, problemset 1 h (default — owner may change)
  - `refresh(force = true)` refreshes regardless of age
- [ ] `StaleWhileRevalidateTest` (Turbine, virtual time, a fake `JudgeSource`, in-memory Room) covers:
  - cache-only when fresh: no source call
  - stale → the cached data emits with `refreshing = true`, then the new data
  - empty cache + `Offline` → `data = null`, `error = Offline`
  - cached data + `RateLimited` → the data is kept and `error = RateLimited`
  - concurrent collectors trigger a single source call
- [ ] Domain ↔ entity mappers live here, with a round-trip test for `Contest` and `Problem`.
- [ ] Hilt `DataModule` binds the repositories. `app/build.gradle.kts` adds `:core:data` and `:core:codeforces`, so the app's Hilt graph checks the `JudgeSource` → repository wiring at compile time.
- [ ] An ADR records the stale-while-revalidate policy: max ages, and errors that never clear data.
- [ ] `core/data/CLAUDE.md` and `API.md`.

## Out of scope
- The `INTERNET` permission: 023 declares it in `core/network`'s manifest. Nothing in M1 reaches the network from the UI.
- UI that consumes the repositories (M2). User/profile caching (M9). List prefetch (M2).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
- Features inject `ContestRepository` / `ProblemRepository` (interfaces); fake them in ViewModel tests. The impls are internal.
- `Cached.data == null` means "never fetched": show the empty state only for a non-null empty list, and a full error state only when `data == null && error != null`.
- A new cached dataset (e.g. users, M9) implements `CachedDataset`, reuses `CachedResource` and adds its max age to ADR 0011.
- Pull-to-refresh calls `refresh(force = true)`; it joins a running refresh instead of starting a second one.
- `app/src/test/kotlin/app/pocketcp/data/DataWiringTest.kt` is what makes the app's Hilt graph check the `JudgeSource` wiring at compile time.
