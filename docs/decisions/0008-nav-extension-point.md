# 0008 — Navigation extension point: features contribute `FeatureEntry` via Hilt multibinding

- **Status**: accepted
- **Context**: Features must not depend on each other or edit a shared nav graph (parallel builders, module rules), yet each one adds a top-level destination and its screens.
- **Decision**:
  - `core/navigation` defines `TopLevelDestination(route, label, icon, order)` and `FeatureEntry` (`destination` + `register(NavGraphBuilder)`), and declares `Set<FeatureEntry>` with `@Multibinds`.
  - A feature binds its entry `@IntoSet` in its own Hilt module. `MainActivity` injects the set, sorts it with `inNavigationOrder()` (by `order`, then route) and hands it to the shell, which builds the `NavHost` and the bottom bar / rail from it.
  - Destinations not yet built are `PlaceholderEntry`s in `app`'s `PlaceholderModule`. A feature replaces one by binding the same route: `MainActivity` drops placeholders whose route a real entry binds (`withoutReplacedPlaceholders()`), so features never edit `Placeholders.kt`. The feature replacing the last placeholder deletes `PlaceholderEntry` and its helpers.
  - Routes are plain strings; the item test tag is `nav_<route>`, the content pane's is `screen_<route>` of the selected destination (Maestro ids), so flows never match a screen's text.
- **Consequences**: Adding a destination never touches the shell. The first entry in order is the start destination and the back target, so Home keeps `order = 0`. Renaming a route breaks Maestro flows. Deep links (M4) add routes through the same `register`.
