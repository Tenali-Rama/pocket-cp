---
id: 019
title: Density, text size and settings controls in the design system
milestone: M1
status: done
deps: []
touches: [core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/Theme.kt, core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/Dimensions.kt, core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/PocketCpTypography.kt, core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/component/Controls.kt, core/designsystem/src/main/kotlin/app/pocketcp/core/designsystem/component/Containers.kt, core/designsystem/src/main/res/values/strings.xml, core/designsystem/src/test/, core/designsystem/API.md, docs/decisions/0006-theme-model.md]
spec: ["15. Settings", "5. Design & Layout", "16. Non-Functional"]
---

## Goal
`PocketCpTheme` takes three new inputs: a UI density, an app text scale and a code text scale. The design system also gets the controls the Settings screen needs: a switch row and a single-choice row.

## Acceptance criteria
- [x] New `PocketCpTheme` parameters:
  - `density: UiDensity` (`Comfortable` default, `Compact`)
  - `textScale: Float = 1f`, which multiplies every text token except code
  - `codeTextScale: Float = 1f`, which multiplies `code` / `codeSmall`

  With the defaults, every existing golden is unchanged: CI shows no changed shots.
- [x] `Compact` reduces the vertical padding of `PcpListItem` and `PcpCard` through the spacing tokens. Touch targets stay ≥ 48 dp: `ComponentA11yTest` gains a compact case.
- [x] Two new controls:
  - `PcpSwitchRow(title, checked, onCheckedChange, supporting?)`: the whole row toggles, it works with `Enter` / `Space`, and it carries the switch role
  - `PcpChoiceRow(title, selected, onClick)`: a radio row in a single-select group
- [x] Screenshots:
  - `controls_<device>_<dark|light>`: all 8 shots, showing on/off/disabled switch rows and a 4-option choice group
  - `components_compact_<device>_<dark|light>`: all 8 shots; the list rows are visibly tighter than in `components_*`
  - `components_textLargest_phone_dark`: `textScale` 1.3, nothing clipped
- [x] ADR 0006 gains a line on density and text scale. `core/designsystem/API.md` lists the new parameters and controls.

## Out of scope
- Reading settings (020).
- App and editor font *face* choice: deferred to M5, together with the editor fonts (default — owner may change). M1 covers sizes and ligatures only.

## Notes for next
- 020 maps settings straight onto `PocketCpTheme(density, textScale, codeTextScale)`; the text-size choice in the `controls` gallery uses 1.3 for "Largest" (the screenshot value). Other steps are 020's call.
- `PcpListItem`, `PcpSwitchRow` and `PcpChoiceRow` share the internal `PcpRow` layout (Containers.kt); it matches M3 `ListItem` geometry at comfortable density, so existing goldens did not move.
- `PcpCard` adds no padding of its own: rows pad themselves, other card content pads `spacing.lg` × `spacing.cardVertical` to follow density.
- `PocketCpTheme.spacing` falls back to comfortable spacing outside the theme (as before); colours and typography still throw there.
- Group `PcpChoiceRow`s in a container with `Modifier.selectableGroup()`; the Settings screen should do the same.
