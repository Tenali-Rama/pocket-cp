---
id: 020
title: Shell applies appearance settings and hides disabled modules
milestone: M1
status: todo
deps: [018, 019]
touches: [app/src/main/kotlin/app/pocketcp/MainActivity.kt, app/src/main/kotlin/app/pocketcp/shell/PocketCpApp.kt, app/src/main/kotlin/app/pocketcp/shell/AppearanceMapping.kt, app/src/test/kotlin/app/pocketcp/shell/ShellThemeTest.kt, app/src/test/kotlin/app/pocketcp/shell/ShellModulesTest.kt, app/src/test/kotlin/app/pocketcp/shell/ShellAppearanceScreenshotTest.kt, app/src/test/screenshots/, app/build.gradle.kts+, app/API.md, core/navigation/src/main/kotlin/app/pocketcp/core/navigation/FeatureEntry.kt, core/navigation/src/test/, core/navigation/build.gradle.kts, core/navigation/API.md]
spec: ["15. Settings", "5. Design & Layout"]
---

## Goal
The app reads `SettingsRepository` and applies it live: theme, text sizes, ligatures and density. Top-level destinations whose `OptionalModule` is disabled are removed from the shell completely (spec §15).

## Acceptance criteria
- [ ] Theme: `System` keeps today's behaviour (`themeModeOf(uiMode)`); `Dark` / `Light` / `Amoled` force their mode.
- [ ] Text sizes: `Small` 0.9, `Default` 1.0, `Large` 1.15, `Largest` 1.3 (default — owner may change). These feed `textScale` / `codeTextScale`, on top of the system font scale.
- [ ] A change in the store recomposes without restarting the activity. The first frame waits for the first settings emission, so the screen never flashes the wrong theme.
- [ ] `FeatureEntry` gains `val module: OptionalModule? get() = null`, so existing entries are unaffected. The shell drops entries whose module is in `disabledModules`.
- [ ] Tests:
  - `ShellThemeTest`: a mapping case for each `ThemePreference` × night mode
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
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
