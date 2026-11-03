# core/parser

## Rules
- Pure Kotlin/JVM (`pocketcp.jvm.library`); no Android, Compose, Hilt or network calls.
- This module owns Codeforces HTML selectors and maps them to the judge-agnostic statement model.
- Parsing failures are part of the public API. Callers fall back to the WebView for the affected page.
- Tests use recorded pages through `core/testing`'s `Fixtures`; they never make live network calls.
