---
id: 020
title: Shell applies appearance settings and hides disabled modules
milestone: M1
status: done
deps: [018, 019]
touches: [app/src/main/kotlin/app/pocketcp/MainActivity.kt, app/src/main/kotlin/app/pocketcp/shell/PocketCpApp.kt, app/src/main/kotlin/app/pocketcp/shell/AppearanceMapping.kt, app/src/test/kotlin/app/pocketcp/shell/, app/src/test/screenshots/, app/build.gradle.kts+, app/API.md, core/navigation/src/main/kotlin/app/pocketcp/core/navigation/FeatureEntry.kt, core/navigation/src/test/, core/navigation/build.gradle.kts, core/navigation/API.md]
spec: ["15. Settings", "5. Design & Layout"]
---

## Goal
The app reads `SettingsRepository` and applies it live: theme, text sizes, ligatures and density. Top-level destinations whose `OptionalModule` is disabled are removed from the shell completely (spec §15).

## Acceptance criteria
- [ ] Theme: `System` keeps today's behaviour (`themeModeOf(uiMode)`); `Dark` / `Light` / `Amoled` force their mode.
- [ ] Text sizes: `Small` 0.9, `Default` 1.0, `Large` 1.15, `Largest` 1.3 (default — owner may change). These feed `textScale` / `codeTextScale`, on top of the system font scale.
- [ ] A change in the store recomposes without restarting the activity. The first frame waits for the first settings emission, so the screen never flashes the wrong theme.
- [ ] `FeatureEntry` gains `val module: OptionalModule? get() = null`, so existing entries are unaffected. The shell drops entries whose module is in `disabledModules`.
- [ ] Stable shell test entries: a new test-only `app/src/test/kotlin/app/pocketcp/shell/ShellTestEntries.kt` defines `shellTestEntries`, the five current destinations (home, contests, problemset, profile, settings) as test `PlaceholderEntry`s with today's routes, labels, icons and orders. It does not read `PlaceholderModule`. `ShellStateTest`, `ShellInsetsTest`, `ShellScreenshotTest` and every new shell test in this task use it, so later features replacing a placeholder (021 first) don't change the shell tests or goldens.
- [ ] Tests:
  - `ShellThemeTest`: a mapping case for each `ThemePreference` × night mode
  - `ShellThemeTest`: with `codeLigatures = false` stored, `PocketCpTheme.typography.code.fontFeatureSettings` read inside the shell disables `calt` and `liga`; with `true` it is `null`
  - `ShellModulesTest`: a fake entry with `module = Stats` shows `nav_<route>` when enabled and is absent from the bar and the rail when disabled
- [ ] Screenshots:
  - `shell_amoled_phone_dark`: pure black background
  - `shell_compact_tablet_dark`
  - `shell_textLargest_phone_dark`: nav labels not clipped

  Existing `shell_<device>_<theme>` goldens are unchanged.
- [ ] `app/API.md` and `core/navigation/API.md` updated.

## Out of scope
- The Settings UI (021). The offline banner (023). `PocketCpShell.kt` (023 edits it).

## Notes for next
- `MainActivity` passes `SettingsRepository.settings` to `PocketCpApp(entries, Flow<UserSettings>)`; nothing is drawn until the first emission. Tests needing real DataStore (e.g. `MainActivityTest`) must `waitUntil` the shell appears.
- An optional module's entry overrides `FeatureEntry.module`; the shell filters with `withoutDisabled(disabledModules)` before `PocketCpShell`, so the NavHost doesn't register it either.
- Toggling a module changes the entries list, which rebuilds the NavHost graph and may reset the back stack to Home. 021 (Settings) should check that toggling from the Settings tab keeps the user there.
- `shellTestEntries` use the production `PlaceholderEntry` class and the `nav_*` / `placeholder_*` strings: a feature replacing a placeholder must keep those strings (or move the test entries to its own) so shell goldens stay unchanged.
- Text-size scales (0.9 / 1.0 / 1.15 / 1.3) live in `AppearanceMapping.kt` (`TextSizePreference.scale`); the Settings UI should reuse them.
