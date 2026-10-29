# core/adaptive API

Window size classes, navigation type and fold posture as one composition local, `LocalWindowLayout`.

Package `app.pocketcp.core.adaptive`. Add with `implementation(project(":core:adaptive"))`.

| Symbol | What it is |
|--------|-----------|
| `ProvideWindowLayout { content }` | Provides `LocalWindowLayout` from `rememberWindowLayout()`. Call once at the app root (the shell, task 006). |
| `LocalWindowLayout` | `compositionLocalOf<WindowLayout>`. Fails if read without a provider; tests and previews provide a `WindowLayout` value directly. |
| `rememberWindowLayout(): WindowLayout` | Live layout: size classes from `currentWindowAdaptiveInfo()`, posture from `WindowInfoTracker`. Updates on resize, rotation and fold changes. |
| `WindowLayout(widthClass, heightClass, posture, navigationType)` | `@Immutable` data class. |
| `WindowWidthClass` / `WindowHeightClass` | `Compact`, `Medium`, `Expanded`. Width 600 / 840 dp, height 480 / 900 dp (Material 3). |
| `NavigationType` | `BottomBar` (compact width) or `Rail` (medium, expanded). `navigationTypeOf(widthClass)`. |
| `DevicePosture` | `Flat`, `Book(hinge: IntRect)` (half-opened vertical fold), `Tabletop(hinge: IntRect)` (half-opened horizontal fold). `hinge` is in window pixels. `devicePostureOf(displayFeatures)`. |

Exposes `androidx.window:window` as an `api` dependency (its `DisplayFeature` is in `devicePostureOf`'s signature).
