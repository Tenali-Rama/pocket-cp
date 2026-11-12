# 0020 — Native TeX typesetter

- **Status**: accepted
- **Context**: Statement math must work offline, without a WebView, and in Robolectric screenshots. Codeforces uses a
  practical subset of TeX rather than the full TeX language.
- **Decision**: Use PocketCP's small Kotlin parser/layout engine in `core/statement/math/`, drawing directly on the
  native Android canvas. It supports the common statement subset and rejects unknown or malformed input to source text.
- **License**: The in-repository implementation is covered by PocketCP's GPL-3.0 license, with no third-party typesetter
  dependency.
- **APK size**: no fonts or libraries, only this module's code. The `perf` artifact measured `release_apk_bytes`
  2148334 on both main (014b02a) and this PR (4148df2): 0 bytes so far, as no app module depends on `core/statement`
  yet and R8 drops it. Expect a code-only cost of tens of KB once the Problem screen uses it.
- **Consequences**: The renderer adds no dependency and needs no R8 rules. Future syntax additions
  belong in the parser tests; do not replace it with a WebView or a native binary without revisiting this tradeoff.
