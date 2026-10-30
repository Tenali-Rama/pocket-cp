---
id: 018
title: Settings store on DataStore in core/datastore
milestone: M1
status: todo
deps: [014]
touches: [core/datastore/, settings.gradle.kts+, gradle/libs.versions.toml, docs/decisions/]
spec: ["4. Platform & Architecture", "15. Settings"]
---

## Goal
`core/datastore` stores the user's settings with Preferences DataStore. It exposes `SettingsRepository.settings: Flow<UserSettings>` and one setter per field. It also declares the `Set<OptionalModule>` multibinding of installed modules, which the Features section of Settings lists.

## Acceptance criteria
- [ ] `UserSettings` has these fields and defaults:
  - `theme`: `ThemePreference` `System` | `Dark` | `Light` | `Amoled`, default `System` (default — owner may change)
  - `appTextSize`: `TextSizePreference` `Small` | `Default` | `Large` | `Largest`, default `Default`
  - `codeTextSize`: same type and default
  - `codeLigatures`: default `true`
  - `density`: `DensityPreference` `Comfortable` | `Compact`, default `Comfortable`
  - `disabledModules: Set<OptionalModule>`: default empty, so every module is on (default — owner may change)
- [ ] Enums are stored by stable keys, not ordinals or names.
- [ ] `SettingsRepositoryTest` (temp-file DataStore, Turbine) covers:
  - each setter round-trips
  - an unknown stored key falls back to the default
  - a corrupted file yields defaults (`ReplaceFileCorruptionHandler`)
  - disabling and re-enabling a module
- [ ] The Hilt `DataStoreModule` provides the repository (singleton) and declares `@Multibinds Set<OptionalModule>` (empty in M1).
- [ ] An ADR covers what goes in DataStore vs Room vs the encrypted store (spec §4).
- [ ] `core/datastore/CLAUDE.md` and `API.md`.

## Out of scope
- Editor, run, AI, focus, data and account settings (their milestones). Applying the settings (020). The UI (021).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
