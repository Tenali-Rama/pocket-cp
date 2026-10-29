---
id: 003
title: Design system tokens, rank colours and dark/light/AMOLED themes
milestone: M0
status: todo
deps: [002]
touches: [core/designsystem/, settings.gradle.kts+, gradle/libs.versions.toml, docs/decisions/]
spec: ["5. Design & Layout", "16. Non-Functional"]
---

## Goal
`core/designsystem/` with colour, type, spacing and shape tokens, Codeforces rank colours, and `PocketCpTheme` in dark (default), light and AMOLED variants.

## Acceptance criteria
- [ ] `PocketCpTheme(mode = Dark | Light | Amoled, content)`; tokens reachable via `PocketCpTheme.colors / typography / spacing / shapes`. Near-monochrome palette; no strong accent other than rank colours.
- [ ] Typography: sans-serif UI family and a bundled JetBrains Mono code family with a ligatures on/off parameter.
- [ ] `RankColorsTest`: rating → rank colour at every boundary (1199/1200, 1399/1400, 1599/1600, 1899/1900, 2099/2100, 2299/2300, 2399/2400, 2599/2600, 2999/3000) and unrated, in all three themes; passes in CI.
- [ ] `ContrastTest`: every text-on-background token pair and every rank colour on the surface colour has ≥ 4.5:1 contrast in all three themes (WCAG AA); passes in CI.
- [ ] Screenshot `themeTokens_<device>_<theme>` (8 via the matrix): swatches of the palette, the type scale (UI + code font) and all rank colours, readable and unclipped on every device. Plus `themeTokens_phone_amoled`: pure black background.
- [ ] `core/designsystem/CLAUDE.md` (rule: no hard-coded colours/sizes outside this module) + `API.md`.

## Out of scope
- Components (task 004). User font/density settings (M1).

## Notes
- UI font: platform sans-serif to keep APK size small (default — owner may change).
- Rank colours follow Codeforces ranks, with per-theme shades where needed to pass contrast.

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
