---
id: 021
title: Settings feature with Appearance and Features sections
milestone: M1
status: done
deps: [018, 019, 020]
touches: [feature/settings/, settings.gradle.kts+, app/build.gradle.kts+, app/src/main/kotlin/app/pocketcp/shell/Placeholders.kt, app/src/main/res/values/strings.xml, app/src/test/kotlin/app/pocketcp/shell/ShellTestEntries.kt, .maestro/smoke.yaml, .maestro/settings.yaml]
spec: ["15. Settings", "5. Design & Layout"]
---

## Goal
The Settings tab is a real feature module. It replaces the placeholder and lets the user change every M1 setting. The Features section turns off any installed optional module.

## Acceptance criteria
- [ ] `feature/settings` contributes the `settings` `FeatureEntry` (order 40, tag `nav_settings`, label "Settings", icon `Icons.Filled.Settings`). `Placeholders.settings`, its `PlaceholderModule` line and the now-unused `placeholder_settings` / `nav_settings` strings are deleted.
- [ ] Shell tests keep a stable entry set: they use `shellTestEntries` (created by 020), not `PlaceholderModule`. The only test edit is the settings line in `ShellTestEntries.kt`, which switches its label to `feature/settings`' label string (same text). No other file under `app/src/test/` changes, `ShellStateTest` still clicks `nav_settings`, and CI shows no changed `shell_*` goldens.
- [ ] Layout:
  - widths ≥ Medium: category list | detail, two panes (default — owner may change)
  - compact width: a category list that opens a detail page with a back arrow
  - categories: Appearance, plus Features only when the installed `Set<OptionalModule>` is not empty
- [ ] The Appearance page has controls for theme (4 choices), app text size, code text size, code ligatures and density. They use `PcpChoiceRow` / `PcpSwitchRow`, and every change writes through `SettingsRepository`.
- [ ] Features lists one switch per installed module, with a one-line description. Off adds the module to `disabledModules`.
- [ ] `SettingsViewModelTest` (fake repository, Turbine): state reflects the stored settings; every setter writes; Features is hidden when the installed set is empty.
- [ ] Screenshots:
  - `settings_<device>_<dark|light>`: all 8. Phone shows the category list; tablet, foldable and desktop show list | Appearance detail with the theme group visible
  - `settings_appearance_phone_<dark|light>`: the detail page with a back arrow
  - `settings_features_tablet_<dark|light>`: 3 fake installed modules, one off
  - `settings_appearance_font150_phone_dark`: nothing clipped
- [ ] Maestro:
  - `smoke.yaml`'s Settings step asserts `settings_category_appearance` instead of the placeholder text
  - new `.maestro/settings.yaml` opens Settings → Appearance, selects Light, takes screenshot `settings_light`, relaunches **without** clearing state, and asserts Light is still selected
- [ ] `feature/settings/CLAUDE.md` and `API.md`.

## Out of scope
- About (022). Editor, Run, AI, Data and Account sections (their milestones). Applying settings to the shell (020).
- App and editor font face choice (M5, see 019).

## Notes for next
- Feature view models are built by their `FeatureEntry` with `viewModel { }` (ADR 0012); screens stay stateless under a `*Route`.
- New settings sections (Editor, Run, AI, Data, Account, About) add a `SettingsCategory`, a page in `CategoryPage` and a list entry; tags `settings_category_<name>`.
- Every `OptionalModule` must have a title and description in `SettingsPages.kt` (exhaustive `when`).
- `ShellTestEntries`' settings entry uses `feature/settings`' `settings_label` for both label and placeholder message.
- Radio selection is `checked` for Maestro (Compose maps non-tab `selected` to checked).
