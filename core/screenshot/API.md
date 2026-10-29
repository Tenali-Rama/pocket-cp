# core/screenshot API

Roborazzi screenshot helpers: one call renders a composable across the QUALITY.md §4 device matrix, in dark and light.

Every Compose module (`pocketcp.android.compose`) already has this module on its test classpath, plus Roborazzi,
Robolectric native graphics and a `ciCheck` that runs Roborazzi verify.

```kotlin
class ContestListScreenshotTest : ScreenshotTest() {
    @Test fun loaded() = captureMatrix("contest_list_loaded", themeWrapper = PocketCpThemeWrapper) { ContestList(sample) }
    @Test fun largeFont() = captureSingle("contest_list_font150", ScreenshotConfig(fontScale = 1.5f)) { ContestList(sample) }
}
```

| Symbol | What it does |
|--------|--------------|
| `ScreenshotTest` | Abstract base class: `AndroidJUnit4` runner on Robolectric `SCREENSHOT_SDK` (34). |
| `captureMatrix(name, themeWrapper = MaterialThemeWrapper) { content }` | 8 captures: every `Device` × dark/light → `<name>_<device>_<theme>.png`. All 8 render even when one fails verify; the first failure is thrown. |
| `captureSingle(name, config = ScreenshotConfig(), themeWrapper) { content }` | One capture → `<name>_<device>_<theme>.png`. For AMOLED (own wrapper) and 1.5× font shots; put the variant in `name`. |
| `ScreenshotConfig(device = Phone, dark = true, fontScale = 1f)` | One configuration; `fileName(name)` gives the golden's file name. |
| `Device` | `Phone` 411×914, `Tablet` 1280×800, `Foldable` 673×841, `Desktop` 1440×900 (dp, rendered at density 1); `id` is the file-name part. |
| `ThemeWrapper` | `@Composable (dark: Boolean, content: @Composable () -> Unit) -> Unit`. |
| `MaterialThemeWrapper` | Default wrapper: Material 3 `darkColorScheme()`/`lightColorScheme()` on a full-window `Surface`. |

Files go to the calling module's `src/test/screenshots/` (Roborazzi `outputDir`, set by the convention).
Outside `recordRoborazziDebug`/`verifyRoborazziDebug` (e.g. a plain `testDebugUnitTest`) captures are skipped.

## CI
- `ciCheck` → `verifyRoborazziDebug`: fails on any pixel diff and on any capture without a committed golden.
- `scripts/ci-collect-shots.sh <outdir>` then writes the current rendering of every shot to `<outdir>/<module>/src/test/screenshots/<name>.png`, uploaded as the `screenshots` artifact that `scripts/factory/shots.sh` reads.
