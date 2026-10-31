# core/model API

The judge-agnostic domain (`Contest`, `Problem`, `User`, `JudgeError`), the `JudgeSource` interface every judge implements, and `OptionalModule`, the switchable modules of spec §15.

Package `app.pocketcp.core.model`. Pure Kotlin/JVM, no DI.

## `JudgeSource`
The only way the data layer reaches a judge (ADR 0009). Every method throws only `JudgeError`.
- `val judge: JudgeId`
- `suspend fun contests(gym: Boolean): List<Contest>`: all contests, or all gyms.
- `suspend fun problemset(): List<Problem>`
- `suspend fun users(handles: List<String>): List<User>`

## Types
- `JudgeId(value: String)`: value class, stable and persisted (e.g. `codeforces`).
- `ContestId(value: Int)`: value class.
- `Contest(id, name, phase: ContestPhase, start: Instant?, duration: Duration, isGym, kind: ContestKind = Unknown)`. `start` is `null` when the judge publishes none. `val division: ContestDivision` is derived from `name` (`ContestDivision.of(name)`), never stored.
- `ContestKind`: `Cf | Icpc | Ioi | Unknown`.
- `ContestDivision`: `Div1 | Div2 | Div3 | Div4 | Div1And2 | Educational | Global | Other`. `Educational` and `Global` win over a division the name also states; `Other` when the name states none (e.g. gyms).
- `ContestPhase`: `Upcoming | Running | Finished | Unknown`.
- `ProblemId(contestId: Int, index: String)`: `display` is `"$contestId$index"` (`4A`, `1234B1`). A problem without a contest id is not representable.
- `Problem(id: ProblemId, name, rating: Int?, tags: List<String>, solvedCount: Int?)`.
- `User(handle, rating: Int?, maxRating: Int?, avatarUrl: String?)`.
- `JudgeError : Exception`, sealed: `Offline(cause?) | RateLimited(cause?) | Unavailable(cause?) | Rejected(message, cause?) | Malformed(cause?)`. `Rejected.message` is shown to the user. `Unavailable`: the judge is down or temporarily unreachable (e.g. HTTP 503); other unexpected statuses are `Malformed`.

## Statements (spec §7.4)
`Statement.kt`. Every type is `@Serializable` with explicit `@SerialName`s (snake_case), cached as JSON (ADR 0017); serial names are stored keys, never change one. Sealed subtypes are told apart by the default `type` discriminator, whose value is the subtype's serial name.
- `StatementLanguage(key)`: `En` (`en`), `Ru` (`ru`). `fromKey(key): StatementLanguage?`, `null` for an unknown key. Keys are persisted: never change one.
- `Statement(problem: ProblemId, language, title, timeLimitMillis: Long?, memoryLimitMegabytes: Int?, inputFile: String?, outputFile: String?, sections: List<StatementSection>, samples: List<Sample>, materials: List<MaterialLink>)`. Limits are `null` when the judge states none; `inputFile`/`outputFile` are `null` for standard input/output.
- `StatementSection(kind: SectionKind, title: String?, blocks: List<StatementBlock>)`. `SectionKind`: `Legend | Input | Output | Interaction | Scoring | Note | Other`.
- `StatementBlock`, sealed: `Paragraph(inlines)` (`paragraph`), `DisplayMath(tex)` (`display_math`), `Image(url, description?)` (`image`), `Table(rows: List<TableRow>)` (`table`), `ListBlock(ordered, items: List<List<StatementBlock>>)` (`list`), `Preformatted(text)` (`preformatted`). `TableRow(cells: List<TableCell>)`, `TableCell(inlines, header: Boolean)`. TeX is stored without delimiters.
- `Inline`, sealed: `Text(text, bold = false, italic = false, monospace = false)` (`text`), `Math(tex)` (`math`), `Link(text, url)` (`link`), `LineBreak` (`line_break`).
- `Sample(input, output)`; `MaterialLink(title, url, kind: MaterialKind)`, `MaterialKind`: `Tutorial | Announcement | Other`.
- `ProblemId` is `@Serializable` too (`contest_id`, `index`).

## `OptionalModule(key: String)`
`LocalCompute` (`local_compute`), `Ai` (`ai`), `StressTester` (`stress_tester`), `PracticeAndStreaks` (`practice_streaks`), `Stats` (`stats`), `OtherJudgesCalendar` (`other_judges_calendar`), `FocusModeAutoStart` (`focus_mode_auto_start`).
- `OptionalModule.fromKey(key): OptionalModule?`: `null` for an unknown key.
- Keys are persisted in settings: never change one.
