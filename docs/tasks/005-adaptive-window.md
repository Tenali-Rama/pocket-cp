---
id: 005
title: Window size classes and fold-posture model in core/adaptive
milestone: M0
status: todo
deps: [001]
touches: [core/adaptive/, settings.gradle.kts+, gradle/libs.versions.toml]
spec: ["5. Design & Layout"]
---

## Goal
One composition-local source of truth for window size class, navigation type and fold posture, so every screen adapts the same way.

## Acceptance criteria
- [ ] `WindowLayout(widthClass, heightClass, posture, navigationType)` exposed by `rememberWindowLayout()` and `LocalWindowLayout`, fed by `currentWindowAdaptiveInfo()` and Jetpack WindowManager `WindowInfoTracker`.
- [ ] `DevicePostureTest`: `FoldingFeature` HALF_OPENED + vertical → `Book(hinge)`, HALF_OPENED + horizontal → `Tabletop(hinge)`, FLAT or none → `Flat`; passes in CI.
- [ ] `NavigationTypeTest`: compact width → `BottomBar`, medium and expanded → `Rail`; passes in CI.
- [ ] `WindowLayoutTest` (Robolectric + window-testing `WindowLayoutInfoPublisherRule`): publishing a half-opened fold updates the composable's posture, and returning to flat updates it back.
- [ ] `core/adaptive/CLAUDE.md` + `API.md`.

## Out of scope
- Any screen. Split-pane layouts using posture (M3/M5).

## Notes
- Breakpoints are the Material 3 window size classes (default — owner may change).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
