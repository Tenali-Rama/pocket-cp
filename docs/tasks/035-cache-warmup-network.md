---
id: 035
title: Warm list caches at start and on reconnect; NetworkMonitor off main and debounced
milestone: M2
status: done
deps: []
touches: [core/data/src/main/kotlin/app/pocketcp/core/data/CacheWarmer.kt, core/data/src/test/kotlin/app/pocketcp/core/data/CacheWarmerTest.kt, core/data/build.gradle.kts, core/data/API.md, core/network/src/main/kotlin/app/pocketcp/core/network/NetworkMonitor.kt, core/network/src/main/kotlin/app/pocketcp/core/network/NetworkModule.kt, core/network/src/test/kotlin/app/pocketcp/core/network/NetworkMonitorTest.kt, core/network/API.md, app/src/main/kotlin/app/pocketcp/PocketCpApplication.kt]
spec: ["3. Performance Budgets", "14. Offline", "2. Product Principles"]
---

## Goal
Lists are already warm when the user opens a tab (spec §3 prefetch). The app refreshes stale contest and problemset caches in the background at start, and again when the connection comes back. This task folds in the #31 follow-ups:
- `NetworkMonitor`'s connectivity calls run off the main thread
- going offline is debounced, so the banner doesn't flash while the default network switches

## Acceptance criteria
- [ ] `ConnectivityNetworkMonitor.isOnline` uses `flowOn` an injected IO dispatcher (detekt `InjectDispatcher`), bound in `NetworkModule`.
- [ ] Debounce: a change to `false` is emitted only if it still holds after 2 s (default — owner may change). Changes to `true` are immediate, and an initial `false` is emitted at once.
- [ ] `NetworkMonitorTest` (virtual time) adds:
  - lose then regain within 2 s emits nothing new
  - a loss held 2 s emits `false`
  - an initial offline state is immediate
  - collection runs on the injected dispatcher

  The existing cases still pass.
- [ ] `CacheWarmer` (core/data, `@Singleton`) has `start()`, which collects `isOnline` in the app scope. On the first `true` and on every later `false → true`, it calls `ContestRepository.refresh(gym = false)` and `ProblemRepository.refresh()`, both non-forced, so fresh caches cost no request. `PocketCpApplication.onCreate` calls `start()`.
- [ ] `CacheWarmerTest` (fakes, Turbine or virtual time) covers: start online refreshes both once; start offline refreshes nothing until online; a reconnect refreshes again; a refresh failure doesn't stop later reconnects.
- [ ] No shell goldens change. `shell_offline_*` still renders through `PocketCpApp(online = false)`.

## Out of scope
- Statement prefetch (M3). Profile prefetch (M9). Paging of standings (M8).

## Notes for next
- `CacheWarmer` takes the repositories as `Provider`s and resolves them on its own scope, so `Application.onCreate` doesn't build OkHttp or Room on the main thread (spec §3 cold start).
- The debounce lives in `ConnectivityNetworkMonitor` (`withIndex` + `transformLatest`): item 0 is never delayed. `OFFLINE_DELAY` = 2 s, overridable through the `offlineDelay` constructor parameter.
- `CacheWarmer` drops anything a refresh throws (repositories shouldn't, per ADR 0011), so an escape can neither crash the app scope nor stop later reconnects.
- `:app` Robolectric tests run the real `PocketCpApplication`, so the warmer starts there too. Robolectric's default network isn't `VALIDATED`, so it never fetches; a test that marks it validated must fake `JudgeSource` first.
- `core/data` now depends on `core/network`, for `NetworkMonitor` only.
