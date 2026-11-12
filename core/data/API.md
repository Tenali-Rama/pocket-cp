# core/data API

Cache-first repositories (stale-while-revalidate, ADR 0011) for contests, the problemset and statements, emitting `Cached<T>` from the Room cache and refreshing it through `JudgeSource`.

Package `app.pocketcp.core.data`. Hilt: `DataModule` provides `ContestRepository`, `ProblemRepository` and `StatementRepository` as singletons, on the app's `JudgeSource` and `core/database` DAOs.
Every repository is an interface with an `internal` implementation: other modules' tests fake the interface.
Depends on `core/network` for `NetworkMonitor` only.

## `Cached<out T>(data: T?, fetchedAt: Instant?, refreshing: Boolean, error: JudgeError?)`
- `data`: the cache; `null` only when the dataset was never fetched. An empty list is a real, fetched empty dataset.
- `fetchedAt`: when the cache was last written, or `null`.
- `refreshing`: a background refresh is running. Show the data, not a spinner, when `data != null`.
- `error`: the last refresh's `JudgeError`; cleared by the next successful refresh. It never clears `data`. A failure that isn't the judge's (e.g. an `SQLiteException` from the cache write) shows as `JudgeError.Unavailable` with it as the cause; it never crashes the refresh scope.

## Policy (every repository)
- Collecting emits the cache at once, then every change to it or to the refresh status.
- Collecting starts a refresh when `fetchedAt` is `null`, older than `MAX_AGE`, or in the future. The first item then already has `refreshing = true`.
- One refresh per dataset at a time: concurrent collectors and `refresh()` calls share it.
- `refresh(force = false)`: refreshes when stale, or always when `force`; returns when it is done. It never throws a `JudgeError`; the failure shows up as `error`.

## `ContestRepository`
- `fun contests(gym: Boolean): Flow<Cached<List<Contest>>>`: newest start first, contests without a start last. Regular and gym are separate datasets.
- `fun activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>>`: regular contests only, bounded for Home. Every running contest, then at most `upcomingLimit` upcoming ones; each group earliest start first, no start last, ties by id. A view of the same dataset as `contests(gym = false)`: same `fetchedAt`, `refreshing`, `error`, staleness and single refresh; `refresh(gym = false)` updates both. `data` is `null` until the first fetch. `IllegalArgumentException` if `upcomingLimit < 0`.
- `suspend fun refresh(gym: Boolean, force: Boolean = false)`
- `MAX_AGE`: 5 minutes.

## `ProblemRepository`
- `fun problemset(): Flow<Cached<List<Problem>>>`: newest contest first, then by index.
- `suspend fun refresh(force: Boolean = false)`
- `MAX_AGE`: 1 hour.

## `StatementRepository`
An interface (implementation `internal`); features and `core/problemview` test against a fake of it. Each problem and language is its own dataset: one Room row, with its own `fetchedAt`, status and single refresh.
- `fun statement(problem: ProblemId, language: StatementLanguage): Flow<Cached<Statement>>`. A row stored in another format version (`STATEMENT_FORMAT_VERSION`, now 1) or that can't be decoded counts as missing: `data = null`, and it is refetched.
- `suspend fun refresh(problem, language, force: Boolean = false)`
- `fun prefetch(problems: List<ProblemId>, language)`: returns at once. In the app-lifetime scope it fetches, one at a time and in order, the problems without a current-format row (stale rows are kept as they are). Stops at the first `JudgeError.Offline`, skips other failures (each shows as that statement's `error`), joins a refresh already running for a problem, and never throws.
- `fun pageUrl(problem, language): String`: `JudgeSource.problemUrl`, for the WebView fallback.
- `MAX_AGE`: 7 days.

## `CacheWarmer` (`@Singleton`, `@Inject`)
- `fun start()`: watches `NetworkMonitor.isOnline` in its own app-lifetime scope. On the first `true` and on every
  later `false → true` it calls `ContestRepository.refresh(gym = false)` and `ProblemRepository.refresh()`, not
  forced, so a fresh cache costs no request. Later calls do nothing. `PocketCpApplication.onCreate` calls it.
- The repositories are resolved lazily on that scope, so `start()` builds neither the HTTP client nor the database.
- A refresh that throws anyway is dropped; it never stops later reconnects.
