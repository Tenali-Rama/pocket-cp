# core/parser API

Package `app.pocketcp.core.parser`. Pure Kotlin/JVM, no Android or network dependency.

## `CodeforcesStatementParser`
- `parse(html: String, problem: ProblemId, language: StatementLanguage): Statement`: parses one Codeforces problem page.
- A Cloudflare challenge throws `StatementParseError.Blocked`.
- Missing `.problem-statement`, a missing title, and malformed markup throw `StatementParseError.UnknownMarkup(detail)`.

## `StatementParseError`
Sealed `Exception` hierarchy for parser failures:
- `Blocked`: the page is a Cloudflare challenge and should be opened in the WebView.
- `UnknownMarkup(detail)`: the page is not a supported Codeforces statement shape.
