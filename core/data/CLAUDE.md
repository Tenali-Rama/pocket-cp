# core/data

## Rules
- The only module that joins the Room cache (`core/database`) and a judge (`JudgeSource`). Features see repositories, `Cached<T>` and `core/model` types; never an entity or a DAO.
- Stale-while-revalidate is ADR 0011: emit the cache first, refresh in the background when stale, keep the data when a refresh fails. A new cached dataset implements `CachedDataset` and reuses `CachedResource`; don't write a second policy.
- Max ages live on the repository interfaces (`MAX_AGE`). Changing one is a product decision: update ADR 0011 with it.
- Never depend on `core/codeforces` or name a judge here (ADR 0009). The app binds the `JudgeSource`.
- Refreshes run in the repository's app-lifetime scope, not the collector's, so one fetch serves every collector.
- Entity ↔ domain mapping lives in `Mappers.kt`, with a round-trip test per type.
- Tests: an in-memory Room database (Robolectric), a `FakeJudgeSource` gated with `CompletableDeferred`, and `SchedulerClock` (virtual time as the cache clock). Turbine with `FLOW_TIMEOUT`, since Room emits from its own threads. No network.
