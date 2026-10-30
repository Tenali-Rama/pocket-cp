---
id: 025
title: Feature entries replace placeholders by route, and the shell tags its content pane
milestone: M2
status: todo
deps: []
touches: [app/src/main/kotlin/app/pocketcp/MainActivity.kt, app/src/main/kotlin/app/pocketcp/shell/Placeholders.kt, app/src/main/kotlin/app/pocketcp/shell/PocketCpShell.kt, app/src/test/kotlin/app/pocketcp/shell/PlaceholderOverrideTest.kt, app/src/test/kotlin/app/pocketcp/shell/ShellStateTest.kt, .maestro/smoke.yaml, app/API.md, core/navigation/API.md, docs/decisions/0008-nav-extension-point.md]
spec: ["4. Platform & Architecture", "5. Design & Layout"]
---

## Goal
Home, Contests and Problemset (026–035) can land in parallel without editing the same hub files. A feature that binds a `FeatureEntry` with a placeholder's route replaces that placeholder automatically, so it never edits `Placeholders.kt`. `smoke.yaml` checks a shell-owned content tag instead of placeholder text, so it never needs editing either.

## Acceptance criteria
- [ ] Before sorting, `MainActivity` drops every `PlaceholderEntry` whose route is also bound by a non-placeholder entry. This is a pure function in `Placeholders.kt`, e.g. `Iterable<FeatureEntry>.withoutReplacedPlaceholders()`.
- [ ] `PlaceholderOverrideTest` (JVM) covers three cases: a real `contests` entry plus the 4 placeholders gives one `contests` entry, the real one; with no real entries, all 4 placeholders stay; two real entries with different routes each replace only their own placeholder.
- [ ] The shell's content pane (the `NavHost` container) has the test tag `screen_<route of the selected top-level destination>`, exposed as a resource id like `nav_*`. It stays the same on nested routes under that destination. `ShellStateTest` asserts `screen_contests` after clicking `nav_contests`.
- [ ] `.maestro/smoke.yaml` asserts `id: screen_<route>` plus the selected `nav_<route>` for every destination. It no longer matches placeholder text. Settings keeps its `settings_category_appearance` check.
- [ ] No `shell_*` golden changes (test tags don't render).
- [ ] ADR 0008 and `core/navigation/API.md` say that a feature replaces its placeholder by binding the same route, and no longer deletes the placeholder line. They also say the last feature to replace a placeholder deletes `PlaceholderEntry` itself (no dead code).

## Out of scope
- Any feature screen. Deleting placeholders that are still in use (Profile stays one until M4/M9).

## Notes for next
<!-- builder fills in -->
