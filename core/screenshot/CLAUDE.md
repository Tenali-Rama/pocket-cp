# core/screenshot

Test-support library for Roborazzi screenshot tests. Other modules only use it as `testImplementation`
(the `pocketcp.android.compose` convention adds it); nothing here ships in the app.

## Rules
- The device matrix and file naming are fixed by `docs/decisions/0005-screenshot-matrix.md` and QUALITY.md §4. Changing a size re-renders every golden in the repo: do it in its own task.
- Goldens live in each module's `src/test/screenshots/` and are produced only by CI. Never hand-edit or hand-merge a PNG; accept CI output with `scripts/factory/shots.sh <pr> --accept` after looking at it.
- Keep the helpers theme-agnostic: the caller passes a `ThemeWrapper`. The design-system wrapper belongs to the design-system module, not here.
- Robolectric runs on `SCREENSHOT_SDK` (34): SDK 35+ needs JDK 21. Native graphics come from the convention, not from per-test annotations.
- `ScreenshotMatrixTest` is the self-test of the helpers; its 8 goldens must keep showing the sample filling each window.
