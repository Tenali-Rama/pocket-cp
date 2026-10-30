# core/designsystem API

Design tokens and `PocketCpTheme` in dark (default), light and AMOLED, with Codeforces rank colours as the only accent; plus the shared components (buttons, chips, cards, list items, top bar, ranked handle, empty/error/loading states).

Package `app.pocketcp.core.designsystem`. Add with `implementation(project(":core:designsystem"))`; it exposes Material 3 as `api`.

```kotlin
PocketCpTheme(mode = ThemeMode.Dark, codeLigatures = true, density = UiDensity.Comfortable, textScale = 1f, codeTextScale = 1f) {
    Text(handle, color = PocketCpTheme.colors.rank.ofRating(user.rating), style = PocketCpTheme.typography.body)
}
```

| Symbol | What it is |
|--------|-----------|
| `PocketCpTheme(mode, codeLigatures = true, density = Comfortable, textScale = 1f, codeTextScale = 1f, content)` | Provides the tokens and a matching `MaterialTheme` (colour scheme, type scale, shapes). Draws nothing: paint `colors.background` yourself. `textScale` multiplies every UI text style (stacks with the system font scale); `codeTextScale` only `code` / `codeSmall`. |
| `UiDensity` | `Comfortable` (default), `Compact`: trims the vertical padding of rows and cards through the `listItem*` and `cardVertical` spacing tokens. Touch targets stay ≥ 48 dp. |
| `PocketCpTheme.colors / typography / spacing / shapes` | The current tokens, read inside `PocketCpTheme`. |
| `ThemeMode` | `Dark`, `Light`, `Amoled` (pure black background and surface). |
| `PocketCpColors` | `background`, `onBackground`, `surface`, `surfaceRaised`, `onSurface`, `onSurfaceMuted`, `outline`, `primary`, `onPrimary`, `error`, `onError`, `rank`, `isDark`. Greys only, except `error` and `rank`. `PocketCpColors.of(mode)` outside composition. |
| `Rank` | Codeforces ranks `Newbie` … `LegendaryGrandmaster`, `Unrated`, with `minRating`. `Rank.ofRating(rating: Int?)`; `null` → `Unrated`. |
| `RankColors` | One shade per rank colour for the theme: `of(rank)`, `ofRating(rating)`. Master = International Master; the three grandmaster ranks share one. |
| `PocketCpTypography` | `display`, `headline`, `title`, `body`, `bodySmall`, `label` (platform sans-serif); `code`, `codeSmall` (JetBrains Mono, ligatures per `codeLigatures`). |
| `PocketCpSpacing` | `xxs` 2, `xs` 4, `sm` 8, `md` 12, `lg` 16, `xl` 24, `xxl` 32 dp; `touchTarget` 48 dp. Density tokens (comfortable / compact): `listItemVertical` 8 / 4, `listItemHeight` 56 / 48, `listItemTwoLineHeight` 72 / 48, `cardVertical` 16 / 8 dp. `PocketCpSpacing.of(density)` outside composition. |
| `PocketCpShapes` | Rounded corners `small` 4, `medium` 8, `large` 12, `extraLarge` 16 dp. |

All tokens are `@Immutable`. Every text token pair and every rank colour on `background`, `surface` and `surfaceRaised` has ≥ 4.5:1 contrast (`ContrastTest`).

Screenshot tests elsewhere wrap content as
`captureMatrix(name, themeWrapper = { dark, c -> PocketCpTheme(if (dark) ThemeMode.Dark else ThemeMode.Light, content = c) }) { … }`
and paint `PocketCpTheme.colors.background` behind the screen.

## Components

Package `app.pocketcp.core.designsystem.component`. Built from the tokens; built-in strings are resources in
`src/main/res/values/strings.xml` (`designsystem_*`). Icons: `androidx.compose.material:material-icons-core`, exposed as `api`.

| Symbol | What it is |
|--------|-----------|
| `PcpPrimaryButton(text, onClick, modifier, enabled)` | Filled main action. |
| `PcpSecondaryButton(text, onClick, modifier, enabled)` | Outlined secondary action. |
| `PcpTextButton(text, onClick, modifier, enabled)` | Text-only, low emphasis. |
| `PcpIconButton(icon, contentDescription, onClick, modifier, enabled)` | Icon-only; `contentDescription` is required and non-null. Touch target ≥ 48 dp. |
| `PcpCard(modifier, onClick = null, enabled, content: ColumnScope)` | `surface` block with a hairline `outline` border; clickable when `onClick` is set. Adds no padding: pad non-row content `spacing.lg` × `spacing.cardVertical`. |
| `PcpListItem(headline, modifier, supporting, leading, trailing, onClick, enabled)` | One transparent list row; disabled rows mute the headline and ignore clicks. Padding and min height follow the density. |
| `PcpSwitchRow(title, checked, onCheckedChange, modifier, supporting, enabled)` | Settings row with a trailing switch; the whole row toggles (tap, `Enter`, `Space`) and carries `Role.Switch`. |
| `PcpChoiceRow(title, selected, onClick, modifier, enabled)` | One radio option, `Role.RadioButton`; wrap a group's rows in `Modifier.selectableGroup()`. |
| `PcpTopAppBar(title, modifier, onNavigateBack = null, actions)` | Flat top bar on `background`; a back arrow when `onNavigateBack` is set. |
| `PcpFilterChip(label, selected, onClick, modifier, enabled)` | Toggle filter with a check mark when selected. Touch target ≥ 48 dp. |
| `RankedHandle(handle, rank \| rating, modifier, style)` | Bold handle in its rank colour; a legendary grandmaster's first letter in `onBackground`. |
| `EmptyState(title, modifier, message, icon, action)` | Centred "nothing here" with an optional action. |
| `ErrorState(message, onRetry, modifier)` | Message and a Retry button; a polite live region for TalkBack. |
| `LoadingPlaceholder(modifier, rows = 5)` | Static skeleton rows read as one "Loading" node. Only when there is no cached data (spec §2). |

Every button, chip, card and clickable row is focusable and activated by `Enter` from a hardware keyboard (`ComponentA11yTest`, `ControlsTest`).
Screenshots: `components_*`, `components_compact_*`, `components_font150_phone_dark`, `components_textLargest_phone_dark`, `controls_*`, `rankedHandle_phone_*`, `states_*`.
