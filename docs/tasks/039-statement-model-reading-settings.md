---
id: 039
title: Statement domain model and reading settings (language, text size, width)
milestone: M3
status: done
deps: []
touches: [core/model/src/main/kotlin/app/pocketcp/core/model/Statement.kt, core/model/src/test/kotlin/app/pocketcp/core/model/StatementTest.kt, core/model/build.gradle.kts, core/model/CLAUDE.md, core/model/API.md, core/datastore/src/main/kotlin/app/pocketcp/core/datastore/UserSettings.kt, core/datastore/src/main/kotlin/app/pocketcp/core/datastore/SettingsRepository.kt, core/datastore/src/main/kotlin/app/pocketcp/core/datastore/DataStoreSettingsRepository.kt, core/datastore/src/test/kotlin/app/pocketcp/core/datastore/SettingsRepositoryTest.kt, core/datastore/API.md, feature/settings/src/test/kotlin/app/pocketcp/feature/settings/FakeSettingsRepository.kt, feature/contests/src/test/kotlin/app/pocketcp/feature/contests/FakeSettingsRepository.kt, feature/problemset/src/test/kotlin/app/pocketcp/feature/problemset/FakeSettingsRepository.kt, gradle/libs.versions.toml, docs/decisions/]
spec: ["7.4 Problem View", "4. Platform & Architecture", "15. Settings"]
---

## Goal
This task adds the judge-agnostic shape of a problem statement, which the parser (040), the cache (042/043) and the renderer (044) all share. It also adds the three persisted reading choices of spec §7.4: statement language, text size and reading width.

## Acceptance criteria
- [ ] `core/model` gets `Statement.kt`. Every type is `@Serializable` with explicit `@SerialName`s, because 043 caches statements as JSON:
  - `StatementLanguage` (`En` key `en`, `Ru` key `ru`) with `fromKey(key): StatementLanguage?`. Keys are persisted: never change one.
  - `Statement(problem: ProblemId, language, title, timeLimitMillis: Long?, memoryLimitMegabytes: Int?, inputFile: String?, outputFile: String?, sections: List<StatementSection>, samples: List<Sample>, materials: List<MaterialLink>)`. `inputFile`/`outputFile` are `null` for standard input/output.
  - `StatementSection(kind: SectionKind, title: String?, blocks: List<StatementBlock>)` with `SectionKind`: `Legend`, `Input`, `Output`, `Interaction`, `Scoring`, `Note`, `Other`.
  - `StatementBlock`, sealed: `Paragraph(inlines)`, `DisplayMath(tex)`, `Image(url, description?)`, `Table(rows: List<TableRow>)` (`TableRow(cells)`, `TableCell(inlines, header: Boolean)`), `ListBlock(ordered, items: List<List<StatementBlock>>)`, `Preformatted(text)`.
  - `Inline`, sealed: `Text(text, bold, italic, monospace)`, `Math(tex)`, `Link(text, url)`, `LineBreak`.
  - `Sample(input, output)`, `MaterialLink(title, url, kind: MaterialKind)` with `MaterialKind`: `Tutorial`, `Announcement`, `Other`.
- [ ] `ProblemId` is `@Serializable` too, and is otherwise unchanged.
- [ ] `core/model/CLAUDE.md` allows `kotlinx-serialization-core` annotations (and nothing else of serialization: no `Json` in main) for persisted domain documents. A new ADR records why: one model, no core/data surrogate (default — owner may change).
- [ ] `StatementTest`: a statement holding every block and inline type round-trips through `Json` (a test-only dependency) unchanged; `StatementLanguage.fromKey` maps both keys and returns `null` for an unknown one.
- [ ] `UserSettings` gets these fields (ADR 0010: stored by key, an unknown value reads as the default):
  - `statementLanguage: StatementLanguage` (`statement_language`, default `En`)
  - `statementTextSize: TextSizePreference` (`statement_text_size`, default `Default`)
  - `statementWidth: StatementWidthPreference` (`statement_width`): `Narrow` (`narrow`), `Comfortable` (`comfortable`, default), `Full` (`full`) (default — owner may change)
- [ ] `SettingsRepository` gets `setStatementLanguage`, `setStatementTextSize` and `setStatementWidth`. `DataStoreSettingsRepository` implements them. The three test fakes (feature/settings, feature/contests, feature/problemset) implement them the way they implement their other setters. Nothing else in the fakes changes.
- [ ] `SettingsRepositoryTest` covers:
  - the defaults
  - each setter's round trip
  - an unknown stored key for each new field reads as its default
- [ ] `core/model/API.md` and `core/datastore/API.md` document the new types and fields.

## Out of scope
- Parsing, caching or rendering statements. A Settings-screen section for the reading choices: they are changed from the problem view (046) (default — owner may change).
- Bookmarks, notes and personal tags (moved to M10 with spec §10's notes and lists).

## Notes for next
- Serial names are snake_case; sealed subtypes use the default `type` discriminator (`paragraph`, `display_math`, `line_break`, …). 043 can use a plain `Json { ignoreUnknownKeys = true }`.
- `ProblemId` serial names are `contest_id` and `index`. Editing `Problem.kt` was outside `touches` but needed for criterion 2.
- `Inline.Text` flags default to `false`, so the parser (040) can write `Inline.Text("x")`.
- `core/model` exports `kotlinx-serialization-core` as `api`; consumers need no serialization plugin to use the types.
- Test fakes use block bodies for the new setters: ktlint (140 cols) forces a one-line expression body, which exceeds detekt's 120.
