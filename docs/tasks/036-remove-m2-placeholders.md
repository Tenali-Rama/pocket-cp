---
id: 036
title: Delete the Home, Contests and Problemset placeholders once their features are on main
milestone: M2
status: done
deps: [030, 032, 034]
touches: [app/src/main/kotlin/app/pocketcp/shell/Placeholders.kt, app/src/main/res/values/strings.xml, app/src/test/kotlin/app/pocketcp/shell/ShellTestEntries.kt, app/src/test/screenshots/, app/build.gradle.kts, app/API.md]
spec: ["4. Platform & Architecture"]
---

## Goal
After 030, 032 and 034, the `home`, `contests` and `problemset` placeholders are always replaced by route (025), so they are dead code. This task deletes them. Profile stays a placeholder until M4/M9. Running it as a separate task keeps 030, 032 and 034 parallel, because none of them edits `Placeholders.kt`.

## Acceptance criteria
- [ ] `Placeholders.home`, `Placeholders.contests`, `Placeholders.problemset`, their `PlaceholderModule` lines, and the `placeholder_home` / `placeholder_contests` / `placeholder_problemset` strings are deleted. `nav_home` / `nav_contests` / `nav_problemset` are deleted too if no `app/src/main` code references them any more.
- [ ] `ShellTestEntries` builds those three entries from the feature modules' label strings (as it already does for Settings with `SettingsR.string.settings_label`), so it no longer references any deleted string. If `app` doesn't have the feature modules as test dependencies yet, add them to `app/build.gradle.kts` as `testImplementation`.
- [ ] Existing `MainActivityTest`, `ShellStateTest` and `PlaceholderOverrideTest` pass unchanged. `MainActivityTest` still finds all five `nav_*` tags, so the real features are bound.
- [ ] Shell goldens: re-recorded only if the home placeholder message changed. `shots.sh` then lists only `shell_*` goldens as changed, and each still shows the selected Home destination with its label. No other module's goldens change.
- [ ] `.maestro/smoke.yaml` passes unchanged.

## Out of scope
- Deleting `PlaceholderEntry` or the Profile placeholder (M4/M9: the last feature to replace a placeholder deletes them, per ADR 0008).

## Notes for next
- Only Profile is still a placeholder; the feature replacing it deletes `PlaceholderEntry`, `Placeholders`, `PlaceholderModule` and `withoutReplacedPlaceholders` (ADR 0008).
- `shellTestEntries` labels Home/Contests/Problemset with the feature modules' `*_label` strings and uses the label as the `EmptyState` message (as Settings does), so shell goldens now read "Home / Home".
- `PlaceholderOverrideTest` (outside touches) builds its placeholders from `shellTestEntries`; it had to change because it named the deleted `Placeholders.*`. Added `placeholderModule_bindsOnlyProfile`.
- M2 closed: app version 0.2.0 / code 3. Release APK 2,146,694 B (budget 2,154,719 B, about 8.0 KB headroom, up from about 5.7 KB).
