# app API

The application module: `PocketCpApplication` (`@HiltAndroidApp`) and the single `MainActivity` (`@AndroidEntryPoint`), which hosts the adaptive app shell.

- Package / namespace / applicationId: `app.pocketcp`.
- Nothing depends on `app`; it depends on `core/*` and `feature/*` modules.

## Shell (`app.pocketcp.shell`)
| Symbol | What it is |
|--------|-----------|
| `MainActivity` | Edge-to-edge; injects `Set<FeatureEntry>`, sorts it with `inNavigationOrder()` and shows `PocketCpApp`. |
| `PocketCpApp(entries, windowInsets = systemBars)` | The Compose root: `PocketCpTheme` from the system night mode, `ProvideWindowLayout`, `testTagsAsResourceId = true` (tag `shell_root`), and the shell. |
| `PocketCpShell(entries, modifier, windowInsets, navController)` | Bottom bar (compact width) or navigation rail (wider) next to a `NavHost` of every entry; the first entry is the start destination. Bar/rail pad themselves by the system-bar insets. |
| `themeModeOf(uiMode)` | `Light` only for `UI_MODE_NIGHT_NO`; `Dark` for night or undefined. |
| `PlaceholderEntry` / `Placeholders` / `PlaceholderModule` | Home (M2), Contests (M2), Problemset (M2), Profile (M9), Settings (M1) as `EmptyState` placeholders. A feature replacing one deletes its line in `PlaceholderModule.placeholders()`. |

Nav items carry test tags `nav_home`, `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` and their label as content description.
Switching tab pops to Home with saved state, so Back from any tab returns to Home, then leaves the app.
Screenshots: `shell_<device>_<theme>`.
