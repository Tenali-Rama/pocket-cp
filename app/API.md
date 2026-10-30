# app API

The application module: `PocketCpApplication` (`@HiltAndroidApp`) and the single `MainActivity` (`@AndroidEntryPoint`), which hosts the adaptive app shell.

- Package / namespace / applicationId: `app.pocketcp`.
- Nothing depends on `app`; it depends on `core/*` and `feature/*` modules.

## Shell (`app.pocketcp.shell`)
| Symbol | What it is |
|--------|-----------|
| `MainActivity` | Edge-to-edge; injects `Set<FeatureEntry>` (sorted with `inNavigationOrder()`) and `SettingsRepository`, and shows `PocketCpApp` on `settings`. Matches the system-bar icons to the resolved theme. |
| `PocketCpApp(entries, settings: Flow<UserSettings>, windowInsets, onDarkTheme)` | Live root: draws nothing until the first emission (no wrong-theme frame), then recomposes on each change. |
| `PocketCpApp(entries, settings: UserSettings = UserSettings(), windowInsets = systemBars, onDarkTheme = {})` | The Compose root: `PocketCpTheme` from the settings (theme, text sizes, ligatures, density), `ProvideWindowLayout`, `testTagsAsResourceId = true` (tag `shell_root`), and the shell with the entries `withoutDisabled(disabledModules)`. |
| `PocketCpShell(entries, modifier, windowInsets, navController)` | Bottom bar (compact width) or navigation rail (wider) next to a `NavHost` of every entry; the first entry is the start destination. Bar/rail pad themselves by the system-bar insets. |
| `themeModeOf(uiMode)` | `Light` only for `UI_MODE_NIGHT_NO`; `Dark` for night or undefined. |
| `themeModeOf(theme, uiMode)` | `System` → `themeModeOf(uiMode)`; `Dark` / `Light` / `Amoled` force their mode. |
| `TextSizePreference.scale` | `Small` 0.9, `Default` 1.0, `Large` 1.15, `Largest` 1.3: `textScale` / `codeTextScale`, on top of the system font scale. |
| `DensityPreference.uiDensity` | The design system's `UiDensity`. |
| `PlaceholderEntry` / `Placeholders` / `PlaceholderModule` | Home (M2), Contests (M2), Problemset (M2), Profile (M9) as `EmptyState` placeholders; Settings is `feature/settings`. A feature replacing one deletes its line in `PlaceholderModule.placeholders()`. |

Nav items carry test tags `nav_home`, `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` and their label as content description.
Switching tab pops to Home with saved state, so Back from any tab returns to Home, then leaves the app.
Shell tests use the test-only `shellTestEntries` (`src/test/.../ShellTestEntries.kt`), never `PlaceholderModule`, so replacing a placeholder leaves them and their goldens alone.
Screenshots: `shell_<device>_<theme>`, `shell_amoled_phone_dark`, `shell_compact_tablet_dark`, `shell_textLargest_phone_dark`.
