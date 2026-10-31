# 0017 — Persisted domain documents are `@Serializable` in `core/model`

- **Status**: accepted (default — owner may change)
- **Context**: Statements (spec §7.4) are deep trees (sections, blocks, inlines) cached as JSON for offline reading (043). A Room column per node is impractical, and a parallel surrogate tree in `core/data` would duplicate every type and its mapping.
- **Decision**:
  - `core/model` may use `kotlinx-serialization-core` annotations, and nothing else of serialization, on domain documents the app persists as JSON. `Json` stays out of main code there.
  - Every property and sealed subtype gets an explicit `@SerialName`, so renaming Kotlin code never changes stored JSON.
- **Consequences**: One model, no surrogate or mapper. `core/model` stays pure JVM but exports the serialization runtime. A `@SerialName` is a stored key: changing one breaks existing caches, so add a new name and bump the cache instead. Judge DTOs still never enter `core/model` (ADR 0009).
