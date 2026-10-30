# core/data API

Cache-first repositories (stale-while-revalidate, ADR 0011) for contests and the problemset, emitting `Cached<T>` from the Room cache and refreshing it through `JudgeSource`.

Package `app.pocketcp.core.data`. Hilt: `DataModule` provides `ContestRepository` and `ProblemRepository` as singletons, on the app's `JudgeSource` and `core/database` DAOs.

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
- `suspend fun refresh(gym: Boolean, force: Boolean = false)`
- `MAX_AGE`: 5 minutes.

## `ProblemRepository`
- `fun problemset(): Flow<Cached<List<Problem>>>`: newest contest first, then by index.
- `suspend fun refresh(force: Boolean = false)`
- `MAX_AGE`: 1 hour.
