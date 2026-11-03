# 0020 — Native TeX typesetter

- **Status**: accepted
- **Context**: Statement math must work offline, without a WebView, and in Robolectric screenshots. Codeforces uses a
  practical subset of TeX rather than the full TeX language.
- **Decision**: Use PocketCP's small Kotlin parser/layout engine in `core/statement/math/`, drawing directly on the
  native Android canvas. It supports the common statement subset and rejects unknown or malformed input to source text.
- **Consequences**: The renderer has zero new dependency or APK-size cost and needs no R8 rules. Future syntax additions
  belong in the parser tests; do not replace it with a WebView or a native binary without revisiting this tradeoff.
