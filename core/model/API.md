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

## `OptionalModule(key: String)`
`LocalCompute` (`local_compute`), `Ai` (`ai`), `StressTester` (`stress_tester`), `PracticeAndStreaks` (`practice_streaks`), `Stats` (`stats`), `OtherJudgesCalendar` (`other_judges_calendar`), `FocusModeAutoStart` (`focus_mode_auto_start`).
- `OptionalModule.fromKey(key): OptionalModule?`: `null` for an unknown key.
- Keys are persisted in settings: never change one.
