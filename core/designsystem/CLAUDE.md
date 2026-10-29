# core/designsystem

## Rules
- The only place with colour, size, type and shape literals. Everywhere else reads `PocketCpTheme.colors / typography / spacing / shapes`: no hard-coded colours or sizes outside this module.
- The palette stays near-monochrome; rank colours are the only strong accent (`docs/decisions/0006-theme-model.md`). Don't add a brand or accent colour.
- Every text-on-background pair and every rank colour must keep ≥ 4.5:1 contrast in all three themes. Adding a colour token means adding its pairs to `ContrastTest`.
- Rank boundaries live in `Rank`; colours in `RankColors`, with a shade per theme. Features never map ratings to colours themselves.
- Token literals sit in `Colors.kt`, `PocketCpTypography.kt` and `Dimensions.kt` (file-level `MagicNumber` suppression); keep them there.
- Fonts: the UI uses the platform sans-serif (keeps the APK small). The code font is JetBrains Mono, bundled in `src/main/assets/fonts/` with its SIL Open Font License, `OFL.txt`, next to it. The licence must ship with the font: never remove or move it away from the `.ttf` files.
- `themeTokens` screenshots (`src/test/screenshots/`) show every token; re-accept them when a token changes.
- Components (buttons, chips, rows) are task 004, not here.
