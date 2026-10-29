# core/adaptive

## Rules
- The single source of truth for window size class, navigation type and fold posture. Screens read `LocalWindowLayout`; they never call `currentWindowAdaptiveInfo()` or `WindowInfoTracker` themselves.
- Breakpoints are the Material 3 window size classes (`WindowSizeClass.*_LOWER_BOUND` from `androidx.window:window-core`). Change them only here.
- Mapping logic stays in plain functions (`devicePostureOf`, `navigationTypeOf`, `widthClass`/`heightClass`) with their own unit tests; the composable only wires inputs to them.
- No screens or layouts here: split panes that use the posture belong to the features (M3/M5).
- Tests: Robolectric on SDK 34 (`src/test/resources/robolectric.properties`). Fake folds come from `window-testing`'s `WindowLayoutInfoPublisherRule`, chained outside the compose rule so the fake tracker is installed before the activity starts.
