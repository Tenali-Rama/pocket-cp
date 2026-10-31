# core/database API

The app's Room database (version 3): cache tables for contests, problems and statements, and `cache_meta` with when each dataset was last fetched.

Package `app.pocketcp.core.database`. Hilt: `DatabaseModule` provides `PocketCpDatabase` (singleton, file `pocketcp.db`), `ContestDao`, `ProblemDao`, `CacheMetaDao` and `StatementDao`.

## Entities
| Entity (table) | Key | Fields |
|----------------|-----|--------|
| `ContestEntity` (`contests`) | `id: Int` | `name`, `phase: String` (`ContestPhase` name), `startEpochSeconds: Long?`, `durationSeconds: Long`, `isGym`, `kind: String` (`ContestKind` name; SQL default `'Unknown'`) |
| `ProblemEntity` (`problems`) | `(contestId: Int, index: String)` | `name`, `rating: Int?`, `tags: List<String>` (JSON array column), `solvedCount: Int?` |
| `CacheMetaEntity` (`cache_meta`) | `key: String` | `fetchedAtEpochMillis: Long` |
| `StatementEntity` (`statements`) | `(contestId: Int, index: String, language: String)` | `document: String` (serialized statement written by `core/data`), `formatVersion: Int` (of that serialization), `fetchedAtEpochMillis: Long`. `language` is the `StatementLanguage` key |

`CacheKeys`: `CONTESTS`, `GYM_CONTESTS`, `PROBLEMSET`; `contests(gym)` picks one of the first two.

## DAOs
- `ContestDao`
  - `fun contests(gym: Boolean): Flow<List<ContestEntity>>`: newest `startEpochSeconds` first, null starts last, ties by `id` descending.
  - `fun activeContests(upcomingLimit: Int): Flow<List<ContestEntity>>`: regular dataset only. Every `Running` contest, then at most `upcomingLimit` `Upcoming` ones; each group earliest `startEpochSeconds` first, null starts last, ties by `id` ascending. `Finished` and `Unknown` are never returned. A negative limit means no limit (SQLite); `core/data` rejects it.
  - `suspend fun replaceAll(gym, contests, fetchedAtEpochMillis)`: one transaction; deletes that dataset, inserts `contests`, writes `CacheKeys.contests(gym)`. The other dataset is untouched. `IllegalArgumentException` if a contest's `isGym != gym`.
- `ProblemDao`
  - `fun problems(): Flow<List<ProblemEntity>>`: `contestId` descending, then `index` ascending.
  - `suspend fun replaceAll(problems, fetchedAtEpochMillis)`: one transaction; replaces the problemset, writes `CacheKeys.PROBLEMSET`.
- `CacheMetaDao`
  - `suspend fun fetchedAt(key: String): Long?`: epoch millis of the last `replaceAll` for that key, or null.
- `StatementDao`: statements are cached one row at a time, so this table has no `replaceAll` and no `cache_meta` row.
  - `fun statement(contestId, index, language): Flow<StatementEntity?>`: that row or null; re-emits after a write to the table.
  - `suspend fun upsert(entity)`: inserts, or replaces the row with the same key.
  - `suspend fun cachedIndices(contestId: Int, language: String, formatVersion: Int): List<String>`: the contest's indices cached in that language with that `formatVersion`, ascending; for prefetch.

Every `Flow` re-emits after a write to its table.

## Migrations
`DatabaseModule` registers every migration; there is no destructive fallback.
- `1 → 2`: adds `contests.kind` (`Unknown` for cached rows) and deletes both contest datasets' `cache_meta` rows, so the next read refetches them. The problemset's row is kept.
- `2 → 3`: creates `statements`. Every cached contest, problem and `cache_meta` row is kept.
