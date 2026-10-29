# 0006 — Theme model: three modes, monochrome palette, rank colours as the only accent

- **Status**: accepted
- **Context**: Spec §5 wants a minimal, dark-first, near-monochrome look with Codeforces rank colours as the only strong accent, plus light and AMOLED themes; §16 wants WCAG AA contrast.
- **Decision**:
  - `ThemeMode` = `Dark` (default) | `Light` | `Amoled`. AMOLED is the dark palette on pure black (`background` = `surface` = #000).
  - The palette is greys only (`primary` is near-white / near-black), plus a muted `error`. No brand accent colour.
  - Rank colours keep the Codeforces hues; each theme has its own shade where the original fails 4.5:1 on that theme's surfaces (e.g. Expert blue is lightened on dark, Master orange darkened on light). Unrated uses the plain text colour.
  - `ContrastTest` enforces ≥ 4.5:1 for every text pair and every rank colour on `background`, `surface`, `surfaceRaised`.
  - UI font: platform sans-serif. Code font: bundled JetBrains Mono (OFL), ligatures toggled with `font-feature-settings`.
- **Consequences**: New colours must pass `ContrastTest` in all three themes. Never use exact Codeforces hex values directly: they fail AA on dark. A user font setting (M1) only swaps families in `PocketCpTypography.kt`.
