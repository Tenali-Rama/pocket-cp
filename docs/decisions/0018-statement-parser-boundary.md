# 0018 — Isolated Codeforces statement parser

- **Status**: accepted
- **Context**: Codeforces problem pages are judge-specific HTML and can change independently of the app UI or API.
- **Decision**: Keep Jsoup selectors and the `StatementParseError` hierarchy in pure-JVM `core/parser`; callers use the WebView for that page after a parse failure.
- **Consequences**: Parser fixes stay isolated and recorded pages pin behavior; fetching, caching, rendering, and `JudgeError` mapping remain outside this module.
