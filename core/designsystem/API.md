# core/designsystem API

Design tokens and `PocketCpTheme` in dark (default), light and AMOLED, with Codeforces rank colours as the only accent.

Package `app.pocketcp.core.designsystem`. Add with `implementation(project(":core:designsystem"))`; it exposes Material 3 as `api`.

```kotlin
PocketCpTheme(mode = ThemeMode.Dark, codeLigatures = true) {
    Text(handle, color = PocketCpTheme.colors.rank.ofRating(user.rating), style = PocketCpTheme.typography.body)
}
```

| Symbol | What it is |
|--------|-----------|
| `PocketCpTheme(mode, codeLigatures = true, content)` | Provides the tokens and a matching `MaterialTheme` (colour scheme, type scale, shapes). Draws nothing: paint `colors.background` yourself. |
| `PocketCpTheme.colors / typography / spacing / shapes` | The current tokens, read inside `PocketCpTheme`. |
| `ThemeMode` | `Dark`, `Light`, `Amoled` (pure black background and surface). |
| `PocketCpColors` | `background`, `onBackground`, `surface`, `surfaceRaised`, `onSurface`, `onSurfaceMuted`, `outline`, `primary`, `onPrimary`, `error`, `onError`, `rank`, `isDark`. Greys only, except `error` and `rank`. `PocketCpColors.of(mode)` outside composition. |
| `Rank` | Codeforces ranks `Newbie` … `LegendaryGrandmaster`, `Unrated`, with `minRating`. `Rank.ofRating(rating: Int?)`; `null` → `Unrated`. |
| `RankColors` | One shade per rank colour for the theme: `of(rank)`, `ofRating(rating)`. Master = International Master; the three grandmaster ranks share one. |
| `PocketCpTypography` | `display`, `headline`, `title`, `body`, `bodySmall`, `label` (platform sans-serif); `code`, `codeSmall` (JetBrains Mono, ligatures per `codeLigatures`). |
| `PocketCpSpacing` | `xxs` 2, `xs` 4, `sm` 8, `md` 12, `lg` 16, `xl` 24, `xxl` 32 dp; `touchTarget` 48 dp. |
| `PocketCpShapes` | Rounded corners `small` 4, `medium` 8, `large` 12, `extraLarge` 16 dp. |

All tokens are `@Immutable`. Every text token pair and every rank colour on `background`, `surface` and `surfaceRaised` has ≥ 4.5:1 contrast (`ContrastTest`).

Screenshot tests elsewhere wrap content as
`captureMatrix(name, themeWrapper = { dark, c -> PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light, content = c) }) { … }`
and paint `PocketCpTheme.colors.background` behind the screen.
