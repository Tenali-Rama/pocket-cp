# core/datastore API

The user's settings (`UserSettings`) on Preferences DataStore behind `SettingsRepository`, and the `Set<OptionalModule>` multibinding of installed optional modules.

Package `app.pocketcp.core.datastore`. Hilt: `DataStoreModule` provides `SettingsRepository` (singleton, file `files/datastore/settings.preferences_pb`) and declares `@Multibinds Set<OptionalModule>`.

## `SettingsRepository`
- `val settings: Flow<UserSettings>`: the current settings, then each change (distinct). Never throws on IO; a corrupted store yields defaults. On a read `IOException` it emits defaults once and keeps re-reading (backoff 100 ms doubling to 5 s), so later changes still arrive. Non-IO errors propagate.
- `suspend fun setTheme(ThemePreference)`, `setAppTextSize(TextSizePreference)`, `setCodeTextSize(TextSizePreference)`, `setCodeLigatures(Boolean)`, `setDensity(DensityPreference)`, `setHideProblemTags(Boolean)`, `setHideProblemRatings(Boolean)`.
- `suspend fun setModuleEnabled(module: OptionalModule, enabled: Boolean)`: removes it from or adds it to `disabledModules`.

## `UserSettings`
| Field | Type | Default |
|-------|------|---------|
| `theme` | `ThemePreference`: `System` (`system`), `Dark` (`dark`), `Light` (`light`), `Amoled` (`amoled`) | `System` |
| `appTextSize` | `TextSizePreference`: `Small` (`small`), `Default` (`default`), `Large` (`large`), `Largest` (`largest`) | `Default` |
| `codeTextSize` | `TextSizePreference` | `Default` |
| `codeLigatures` | `Boolean` | `true` |
| `density` | `DensityPreference`: `Comfortable` (`comfortable`), `Compact` (`compact`) | `Comfortable` |
| `disabledModules` | `Set<OptionalModule>` | empty: every module on |
| `hideProblemTags` | `Boolean` (`hide_problem_tags`): hide tags in the problemset (spec §7.3) | `false` |
| `hideProblemRatings` | `Boolean` (`hide_problem_ratings`): hide ratings in the problemset (spec §7.3) | `false` |

The enums implement `StoredPreference` (`key: String`); the key in brackets is what is stored.

## Installed modules
`Set<OptionalModule>`: each optional module's own Hilt module adds itself with `@Provides @IntoSet`. Empty in M1. Settings' Features section lists it; a module is shown when installed and not in `disabledModules`.
