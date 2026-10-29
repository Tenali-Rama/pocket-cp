---
id: 004
title: Core design-system components with screenshot tests
milestone: M0
status: done
deps: [003]
touches: [core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/component/, core/designsystem/src/main/res/, core/designsystem/src/test/, core/designsystem/build.gradle.kts, gradle/libs.versions.toml, core/designsystem/API.md]
spec: ["5. Design & Layout", "16. Non-Functional", "2. Product Principles"]
---

## Goal
The shared components early screens need, all built from tokens and covered by screenshots.

## Acceptance criteria
- [ ] Components: buttons (primary / secondary / text), icon button (contentDescription is a required non-null param), card, list item, top app bar, filter chip, `RankedHandle` (rank-coloured handle; legendary grandmaster first letter in the contrast colour), `EmptyState`, `ErrorState` (message + retry), `LoadingPlaceholder` (skeleton rows, not a blank spinner).
- [ ] `ComponentA11yTest`: icon button and chip touch targets ≥ 48 dp; buttons are focusable and `Enter` from a hardware key event triggers `onClick`; passes in CI.
- [ ] Screenshot `components_<device>_<theme>` (8): a gallery of every component, enabled and disabled, nothing clipped.
- [ ] Screenshot `rankedHandle_phone_dark` and `rankedHandle_phone_light`: one handle per rank, newbie to LGM.
- [ ] Screenshot `states_<device>_<theme>` (8): empty, error and loading states side by side on tablet/desktop, stacked on phone.
- [ ] Screenshot `components_phone_dark_font150`: gallery at 1.5× font scale, no clipped text.
- [ ] Every built-in UI string (the `ErrorState` retry label, the `LoadingPlaceholder` content description, default icon descriptions) is a string resource in `core/designsystem/src/main/res/values/strings.xml`. Component code has no string literals. Icons come from `androidx.compose.material:material-icons-core` via the catalog (default — owner may change).
- [ ] `core/designsystem/API.md` lists each component.

## Out of scope
- Navigation bar / rail (task 006). Feature-specific widgets.

## Notes for next
- Components are in `app.pocketcp.core.designsystem.component`; M3-like ones carry a `Pcp` prefix (`PcpCard`, `PcpTopAppBar`, …) to avoid clashing with Material 3 names. The task-named ones (`RankedHandle`, `EmptyState`, `ErrorState`, `LoadingPlaceholder`) keep their names.
- The 1.5x shot is `components_font150_phone_dark.png`, not `components_phone_dark_font150`: `captureSingle` always appends `_<device>_<theme>` and core/screenshot's API says to put the variant in the name.
- `PocketCpThemeWrapper` (dark/light `PocketCpTheme`) is test-internal in `ComponentGallery.kt`. Promote it to core/screenshot or a test-fixtures module if features need it.
- Keyboard tests must call `LocalInputModeManager.current.requestInputMode(InputMode.Keyboard)` first: clickables aren't focusable in touch mode (see `ComponentA11yTest`).
- States and RankedHandle have their own shots (`states_*`, `rankedHandle_*`), so the `components` gallery shows only three sample handles.
