---
id: 005
title: Window size classes and fold-posture model in core/adaptive
milestone: M0
status: done
deps: [001]
touches: [core/adaptive/, settings.gradle.kts+, gradle/libs.versions.toml]
spec: ["5. Design & Layout"]
---

## Goal
One composition-local source of truth for window size class, navigation type and fold posture, so every screen adapts the same way.

## Acceptance criteria
- [x] `WindowLayout(widthClass, heightClass, posture, navigationType)` exposed by `rememberWindowLayout()` and `LocalWindowLayout`, fed by `currentWindowAdaptiveInfo()` and Jetpack WindowManager `WindowInfoTracker`.
- [x] `DevicePostureTest`: `FoldingFeature` HALF_OPENED + vertical → `Book(hinge)`, HALF_OPENED + horizontal → `Tabletop(hinge)`, FLAT or none → `Flat`; passes in CI.
- [x] `NavigationTypeTest`: compact width → `BottomBar`, medium and expanded → `Rail`; passes in CI.
- [x] `WindowLayoutTest` (Robolectric + window-testing `WindowLayoutInfoPublisherRule`): publishing a half-opened fold updates the composable's posture, and returning to flat updates it back.
- [x] `core/adaptive/CLAUDE.md` + `API.md`.

## Out of scope
- Any screen. Split-pane layouts using posture (M3/M5).

## Notes
- Breakpoints are the Material 3 window size classes (default — owner may change).

## Notes for next
- The shell (006) wraps its root in `ProvideWindowLayout { }` and reads `LocalWindowLayout.current.navigationType` for bottom bar vs rail.
- `LocalWindowLayout` has no default: screenshot tests and previews must provide a `WindowLayout(...)` value with `CompositionLocalProvider`.
- `DevicePosture.Book/Tabletop.hinge` is an `IntRect` in window pixels; split panes (M3/M5) convert it with `LocalDensity`.
- Breakpoints are the Material 3 ones via `WindowSizeClass.*_LOWER_BOUND` (androidx.window 1.4.0); change them only in `WindowSizeClasses.kt`.
- Fold tests: chain `WindowLayoutInfoPublisherRule` outside the compose rule and publish only after `waitForIdle()` (the fake flow does not replay).
