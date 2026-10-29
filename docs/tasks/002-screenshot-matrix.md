---
id: 002
title: Roborazzi screenshot matrix, golden gate and CI shot collection
milestone: M0
status: done
deps: [001]
touches: [build-logic/convention/, core/screenshot/, settings.gradle.kts+, gradle/libs.versions.toml, scripts/ci-collect-shots.sh, docs/decisions/]
spec: ["5. Design & Layout"]
---

## Goal
Every module can write a one-line screenshot test that renders across the QUALITY.md §4 matrix, CI fails on any diff or missing golden, and CI uploads the current renderings for `shots.sh`.

## Acceptance criteria
- [ ] `core/screenshot/` exposes a helper (e.g. `captureMatrix(name, themeWrapper) { content }`) that renders phone portrait, tablet landscape, foldable (unfolded), desktop window × dark and light, naming files `<name>_<device>_<theme>.png` under the calling module's `src/test/screenshots/`. Also supports a single-config capture (used for AMOLED and 1.5× font shots).
- [ ] Robolectric native graphics enabled for all screenshot tests via the convention plugin.
- [ ] Self-test `ScreenshotMatrixTest` captures a sample composable; its 8 goldens are committed in `core/screenshot/src/test/screenshots/` and each shows the sample filling the device frame at the right size and theme.
- [ ] `ciCheck` runs Roborazzi verify and fails on any pixel diff **and** on any screenshot test without a committed golden.
- [ ] `scripts/ci-collect-shots.sh <outdir>` writes the current rendering of every screenshot test (also when `ciCheck` failed) to `<outdir>/<module>/src/test/screenshots/<name>.png`; exactly one file per shot, no `_compare`/`_actual` suffixes.
- [ ] Proof in the PR: a CI run with one golden deleted where `verify` failed and `scripts/factory/shots.sh <pr>` listed it as `NEW` (link + output), then the golden restored and CI green.
- [ ] `core/screenshot/CLAUDE.md` + `API.md`; ADR with the device dimensions and naming scheme.

## Out of scope
- Design-system theme (task 003): the helper takes a theme wrapper; default is plain `MaterialTheme` dark/light.

## Notes
- Device sizes (default — owner may change): phone 411×914 dp, tablet landscape 1280×800 dp, foldable unfolded 673×841 dp, desktop 1440×900 dp.
- Goldens are produced only by CI: push, then `shots.sh <pr> --accept` after looking at them.

## Notes for next
- Every `pocketcp.android.compose` module already has `core:screenshot` on its test classpath; a screen test is `class XScreenshotTest : ScreenshotTest() { @Test fun loaded() = captureMatrix("x_loaded", themeWrapper) { X() } }`.
- Task 003: pass the design-system theme as a `ThemeWrapper` (`(dark, content) -> Unit`); AMOLED is `captureSingle("x_amoled", ScreenshotConfig(dark = true), amoledWrapper)`, large font `ScreenshotConfig(fontScale = 1.5f)` with `_font150` in the name.
- First CI run of a new screen is always red (goldens missing, by design); look at `shots.sh` output, `--accept`, push.
- `scripts/factory/shots.sh` exits 1 (pipefail on `find .../ui-*` with no ui artifacts), so its summary line is missing and `--accept` reports failure although it copied the files. Fix in a factory chore.
- Robolectric native graphics are on for every unit test of Compose modules (convention); tests run on SDK 34 via `ScreenshotTest` / `robolectric.properties`.
