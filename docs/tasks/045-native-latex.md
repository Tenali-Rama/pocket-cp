---
id: 045
title: Native LaTeX typesetting for statement math
milestone: M3
status: todo
deps: [044]
touches: [core/statement/src/main/kotlin/app/pocketcp/core/statement/math/, core/statement/src/test/kotlin/app/pocketcp/core/statement/math/, core/statement/src/test/screenshots/, core/statement/build.gradle.kts, core/statement/API.md, core/statement/CLAUDE.md, app/proguard-rules.pro, gradle/libs.versions.toml, config/perf-budgets.json, docs/decisions/]
spec: ["7.4 Problem View", "3. Performance Budgets"]
---

## Goal
Spec §7.4: "LaTeX, so math is never broken". After this task, statement math (`Inline.Math` and `DisplayMath`) is typeset natively, without a WebView. Inline math is aligned on the text baseline and sized with the text, and display math is centred. Bad TeX degrades to its source and never to a blank or a crash.

## Acceptance criteria
- [ ] A new ADR picks a native TeX typesetter. It must:
  - run without a WebView
  - have a GPL-3.0-compatible licence
  - render on Robolectric for screenshots
  - cover Codeforces' common TeX: `\frac`, `\sum`/`\prod` with limits, sub/superscripts, `\le \ge \ne \cdot \ldots \dots`, `\lfloor\rfloor`, `\binom`, `\sqrt`, `\text`, `\mathcal`, `\oplus`, `matrix` / `cases`

  The ADR states its APK size cost. Any keep rules R8 needs go in `app/proguard-rules.pro` with a comment.
- [ ] `core/statement/math/` provides the `LocalMathRenderer` implementation, set as the module's default:
  - inline math is sized from the surrounding text style (so `textSize` and system font scale apply), drawn in the theme's on-surface colour, with its baseline on the text baseline
  - display math is centred, and scrolls horizontally when wider than the column
  - typeset results are cached by `(tex, size, colour)`, so scrolling doesn't re-typeset
- [ ] TeX the typesetter rejects renders as its source in the code font, as 044's fallback does. `MathRendererTest` asserts this for an unbalanced `\frac{` and an unknown macro.
- [ ] `MathRendererTest` also covers:
  - the same formula at the same size is typeset once (cache hit)
  - the content description is the TeX source
- [ ] Screenshots:
  - `statementMath_<device>_<theme>`: inline formulas in a paragraph, baseline-aligned; display math with `\sum` limits, a fraction and a `cases` block
  - `statementMath_font150_phone_dark`
  - `statementMath_textLargest_phone_dark`
  - `statementMath_invalid_phone_dark`: the source fallback

  `statementGallery_*` goldens are re-accepted with typeset math.
- [ ] If the release APK goes over its budget, the PR raises `release_apk_bytes` under ADR 0013, with the reason (the typesetter's fonts and code) and the `perf` artifact values.

## Out of scope
- Any change outside `core/statement`'s math package, besides the module's build file, docs and R8 rules. Math in blogs or editorials (M9).

## Notes for next
<!-- builder fills in -->
