# core/navigation

## Rules
- The extension point between features and the app shell (`docs/decisions/0008-nav-extension-point.md`). Only types live here: no screens, no shell UI, no concrete destinations.
- A feature adds a top-level destination by binding a `FeatureEntry` into the `Set<FeatureEntry>` multibinding (`@IntoSet`) in its own Hilt module. Never edit the app's nav graph or shell for it.
- Routes are lower-case, unique and stable: `TopLevelDestination.testTag` (`nav_<route>`) is what Maestro flows select on. Renaming a route breaks those flows.
- Ordering lives in `inNavigationOrder()` only; the shell never sorts entries itself.
- Tests: Robolectric on SDK 34 (`src/test/resources/robolectric.properties`).
