---
id: 022
title: About page with the unofficial-client disclaimer
milestone: M1
status: done
deps: [021]
touches: [feature/settings/, .maestro/settings.yaml]
spec: ["17. Play Store Compliance", "15. Settings"]
---

## Goal
Settings gets an About category: the app name and version, a clear statement that PocketCP is unofficial and not affiliated with Codeforces (spec §17), and the GPL-3.0 licence.

## Acceptance criteria
- [ ] About is always the last category, added to 021's category list, screen and ViewModel (`SettingsViewModelTest` gains a case: About is last, with and without Features). It shows:
  - app name and `versionName` (from `PackageManager`, not hard-coded)
  - disclaimer: "PocketCP is an unofficial client. It is not affiliated with, endorsed by or connected to Codeforces." (default wording — owner may change)
  - "Licensed under GPL-3.0"
- [ ] No Codeforces logo or branding is used. All strings are resources.
- [ ] `AboutTest` covers: the version is read from the package; the disclaimer node is present and readable by TalkBack.
- [ ] Screenshots:
  - `about_<device>_<dark|light>`: all 8. Two-pane widths show list | About; phone shows the About page
  - `about_font150_phone_dark`: the disclaimer wraps without clipping

  Expected golden changes: every `settings_<device>_<dark|light>` and `settings_features_tablet_<dark|light>` shot gains the About row at the end of the category list. No other existing golden changes.
- [ ] `.maestro/settings.yaml` opens Settings → About, asserts the disclaimer text, and takes screenshot `settings_about`.

## Out of scope
- Open-source licence listing for dependencies, privacy policy (M14).

## Notes for next
- About is `SettingsCategory.About`, always last; a new category goes before it in `SettingsUiState.categories`.
- Name and version come from `Context.installedAppInfo()` (package label + `versionName`), read on `Dispatchers.IO` via `produceState` in `SettingsRoute`; `SettingsScreen` takes `app: AppInfo?` (null until read, About then omits name and version) so screenshots pass a fixed one.
- Disclaimer wording is `settings_about_disclaimer` (default text from the task); the Maestro flow asserts the same text, so change both together.
- M14's dependency licences and privacy policy belong on the About page (append below the licence line).
