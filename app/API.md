# app API

The application module: `PocketCpApplication` (`@HiltAndroidApp`) and the single `MainActivity` (`@AndroidEntryPoint`), which hosts the adaptive app shell.

- Package / namespace / applicationId: `app.pocketcp`.
- Nothing depends on `app`; it depends on `core/*` and `feature/*` modules.

## Judge binding (`app.pocketcp.judge`, ADR 0015)
| Symbol | What it is |
|--------|-----------|
| `LiveJudgeSourceModule` (`src/live/`) | Binds `JudgeSource` to `CodeforcesJudgeSource` on the app's `CodeforcesApi` (singleton). `release`, `benchmarkRelease`, `nonMinifiedRelease`. |
| `FixtureJudgeSourceModule` (`src/debug/`) | Binds `JudgeSource` to a `FixtureJudgeSource` over the debug assets (singleton). |
| `FixtureJudgeSource(assets: FixtureAssets, clock, scope, dispatcher)` (debug) | The real `CodeforcesJudgeSource` mapping over `AssetTransport` (`<method>.json`, `<method>.gym.json` for `gym=true`; missing file → HTTP 404 → `JudgeError.Malformed`). Contest starts move by `now - (startTimeSeconds + relativeTimeSeconds)` of `contest.list.json`; phases stay as recorded. Judge id `codeforces`. |
| `FixtureAssets` (debug) | `fun read(name): String`, throwing `FileNotFoundException` when missing. |

## Shell (`app.pocketcp.shell`)
| Symbol | What it is |
|--------|-----------|
| `MainActivity` | Edge-to-edge; injects `Set<FeatureEntry>` (`withoutReplacedPlaceholders()`, then `inNavigationOrder()`), `SettingsRepository` and `NetworkMonitor`, and shows `PocketCpApp` on `settings` and `isOnline`. Matches the system-bar icons to the resolved theme. |
| `PocketCpApp(entries, settings: Flow<UserSettings>, isOnline: Flow<Boolean> = always true, windowInsets, onDarkTheme)` | Live root: draws nothing until the first settings emission (no wrong-theme frame), then recomposes on each change. Online until `isOnline` first emits. |
| `PocketCpApp(entries, settings: UserSettings = UserSettings(), online = true, windowInsets = systemBars, onDarkTheme = {})` | The Compose root: `PocketCpTheme` from the settings (theme, text sizes, ligatures, density), `ProvideWindowLayout`, `testTagsAsResourceId = true` (tag `shell_root`), and the shell with the entries `withoutDisabled(disabledModules)`. |
| `PocketCpShell(entries, modifier, offline = false, windowInsets, navController)` | Bottom bar (compact width) or navigation rail (wider) next to a `NavHost` of every entry; the first entry is the start destination. The content pane (the `NavHost`) has test tag `screen_<route>` of the selected destination, also on nested routes (in its nested graph, or `<route>/...`). Bar/rail pad themselves by the system-bar insets. While `offline`, `OfflineBanner` (tag `offline_banner`, polite live region, "Offline · showing saved data") tops the content pane: under the status bar, never over the bar or rail. |
| `themeModeOf(uiMode)` | `Light` only for `UI_MODE_NIGHT_NO`; `Dark` for night or undefined. |
| `themeModeOf(theme, uiMode)` | `System` → `themeModeOf(uiMode)`; `Dark` / `Light` / `Amoled` force their mode. |
| `TextSizePreference.scale` | `Small` 0.9, `Default` 1.0, `Large` 1.15, `Largest` 1.3: `textScale` / `codeTextScale`, on top of the system font scale. |
| `DensityPreference.uiDensity` | The design system's `UiDensity`. |
| `PlaceholderEntry` / `Placeholders` / `PlaceholderModule` | Home (M2), Contests (M2), Problemset (M2), Profile (M9) as `EmptyState` placeholders; Settings is `feature/settings`. A feature replaces one by binding an entry with the same route; it never edits this file. |
| `Iterable<FeatureEntry>.withoutReplacedPlaceholders()` | Drops each `PlaceholderEntry` whose route a non-placeholder entry also binds. The feature replacing the last placeholder deletes `PlaceholderEntry`, `Placeholders`, `PlaceholderModule` and this function (no dead code). |

Nav items carry test tags `nav_home`, `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` and their label as content description. Maestro checks `nav_<route>` selected plus `screen_<route>`, never a screen's text.
Switching tab pops to Home with saved state, so Back from any tab returns to Home, then leaves the app.
Shell tests use the test-only `shellTestEntries` (`src/test/.../ShellTestEntries.kt`), never `PlaceholderModule`, so replacing a placeholder leaves them and their goldens alone.
Screenshots: `shell_<device>_<theme>`, `shell_amoled_phone_dark`, `shell_compact_tablet_dark`, `shell_textLargest_phone_dark`, `shell_offline_<device>_<theme>`, `shell_offline_font150_phone_dark`.
