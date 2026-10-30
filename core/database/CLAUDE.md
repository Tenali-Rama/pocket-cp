# core/database

## Rules
- **Migration rule:** any schema change (entity, column, index, table) bumps `PocketCpDatabase.version`, adds a `Migration`, and adds a migration test from the previous version's committed schema (`MigrationTestHelper`). Never use `fallbackToDestructiveMigration`: users' notes and code will live here too (spec §14).
- The exported schema in `schemas/` is committed. A build regenerates it; commit the regenerated file with the version bump, and never edit an older version's file.
- Entities mirror `core/model`'s field names but stay plain: no dependency on `core/model`, no domain types. Mapping lives in `core/data` (017). Enum-like fields are stored as their `name` strings.
- A cached dataset is swapped as a whole with its DAO's `@Transaction replaceAll(...)`, which also writes its `cache_meta` row under a `CacheKeys` key. Don't add piecemeal inserts for cache tables.
- Reads are `Flow` and writes `suspend`, so Room runs them off the main thread. No `allowMainThreadQueries`, also not in tests.
- Tests: an in-memory database under Robolectric (`inMemoryDatabase()`), Turbine for `Flow` re-emission. No fixtures needed; no network.
