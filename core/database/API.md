# core/database API

The app's Room database (version 1): cache tables for contests and problems, and `cache_meta` with when each dataset was last fetched.

Package `app.pocketcp.core.database`. Hilt: `DatabaseModule` provides `PocketCpDatabase` (singleton, file `pocketcp.db`), `ContestDao`, `ProblemDao` and `CacheMetaDao`.

## Entities
| Entity (table) | Key | Fields |
|----------------|-----|--------|
| `ContestEntity` (`contests`) | `id: Int` | `name`, `phase: String` (`ContestPhase` name), `startEpochSeconds: Long?`, `durationSeconds: Long`, `isGym` |
| `ProblemEntity` (`problems`) | `(contestId: Int, index: String)` | `name`, `rating: Int?`, `tags: List<String>` (JSON array column), `solvedCount: Int?` |
| `CacheMetaEntity` (`cache_meta`) | `key: String` | `fetchedAtEpochMillis: Long` |

`CacheKeys`: `CONTESTS`, `GYM_CONTESTS`, `PROBLEMSET`; `contests(gym)` picks one of the first two.

## DAOs
- `ContestDao`
  - `fun contests(gym: Boolean): Flow<List<ContestEntity>>`: newest `startEpochSeconds` first, null starts last, ties by `id` descending.
  - `suspend fun replaceAll(gym, contests, fetchedAtEpochMillis)`: one transaction; deletes that dataset, inserts `contests`, writes `CacheKeys.contests(gym)`. The other dataset is untouched. `IllegalArgumentException` if a contest's `isGym != gym`.
- `ProblemDao`
  - `fun problems(): Flow<List<ProblemEntity>>`: `contestId` descending, then `index` ascending.
  - `suspend fun replaceAll(problems, fetchedAtEpochMillis)`: one transaction; replaces the problemset, writes `CacheKeys.PROBLEMSET`.
- `CacheMetaDao`
  - `suspend fun fetchedAt(key: String): Long?`: epoch millis of the last `replaceAll` for that key, or null.

Every `Flow` re-emits after a `replaceAll` of its table.
