---
id: 006
title: Adaptive app shell with navigation extension point
milestone: M0
status: done
deps: [004, 005]
touches: [core/navigation/, app/src/main/, app/src/test/, app/build.gradle.kts+, settings.gradle.kts+, gradle/libs.versions.toml, app/API.md, docs/decisions/]
spec: ["5. Design & Layout", "2. Product Principles"]
---

## Goal
The app opens into a shell with bottom navigation on phones and a navigation rail on larger windows. Feature modules add destinations by contributing a Hilt `@IntoSet` entry, so they never edit the nav graph.

## Acceptance criteria
- [ ] `core/navigation/`: `TopLevelDestination` (route, label, icon, order) and `FeatureEntry` (destination + `NavGraphBuilder` registration), collected via Hilt multibinding. `FeatureEntryOrderTest` checks entries are ordered by `order`.
- [ ] Placeholder entries in `app` for Home, Contests, Problemset, Profile, Settings, each showing `EmptyState` naming the milestone that delivers it. Later features replace a placeholder by deleting one binding line.
- [ ] Nav items carry test tags `nav_home`, `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` and content descriptions.
- [ ] `ShellStateTest`: selected destination survives `StateRestorationTester` recreation; back from a non-home destination returns to Home; `Tab` + `Enter` on a hardware keyboard switches destination.
- [ ] The Compose root sets `Modifier.semantics { testTagsAsResourceId = true }` so that Maestro (task 010) sees test tags as ids. `ShellStateTest` asserts that the root node has `SemanticsProperties.TestTagsAsResourceId == true`.
- [ ] Edge-to-edge: `MainActivity` calls `enableEdgeToEdge()`. `ShellInsetsTest` (Robolectric, fake insets of 48 dp bottom and 24 dp start) asserts that on phone the bottom bar's bottom padding equals the navigation-bar inset, and on tablet the rail's start padding equals the system-bar inset.
- [ ] Theme follows system dark/light, dark when unknown. `ShellThemeTest` covers dark, light and unspecified.
- [ ] Screenshot `shell_<device>_<theme>` (8): phone shows a bottom bar with 5 items and Home selected; tablet landscape, foldable and desktop show a rail with the same items; content area not stretched into a single narrow column.
- [ ] `core/navigation/CLAUDE.md` + `API.md`; `app/API.md` updated; ADR on the nav extension point.

## Out of scope
- Real feature screens, settings persistence (M1), deep links (M4).

## Notes
- Top-level destinations Home / Contests / Problemset / Profile / Settings (default — owner may change).

## Notes for next
- A feature replaces a placeholder: bind its `FeatureEntry` `@IntoSet` in its own module, keep the route and `order` (Home 0, Contests 10, Problemset 20, Profile 30, Settings 40), and delete its line in `app/.../shell/Placeholders.kt` `PlaceholderModule.placeholders()`.
- Maestro (010) can select `nav_home` … `nav_settings` as resource ids; the root sets `testTagsAsResourceId`.
- `PocketCpApp` takes `windowInsets` (default `systemBars`); tests fake insets through it. Bar/rail pad themselves; content gets the remaining sides.
- Theme is `themeModeOf(uiMode)` from the system; settings persistence (M1) should replace that call with the stored `ThemeMode`.
- `testTagsAsResourceId` lives in `SemanticsPropertiesAndroid`, not `SemanticsProperties` as the criterion said.
