# core/navigation API

The extension point of the app shell: `TopLevelDestination` and `FeatureEntry`, which features contribute through a Hilt `Set<FeatureEntry>` multibinding.

Package `app.pocketcp.core.navigation`. Add with `implementation(project(":core:navigation"))`; it exposes navigation-compose and `core:model` as `api`.

```kotlin
class ContestsEntry @Inject constructor() : FeatureEntry {
    override val destination = TopLevelDestination("contests", R.string.contests_title, Icons.Filled.DateRange, order = 10)
    override fun register(graph: NavGraphBuilder) = graph.composable(destination.route) { ContestsScreen() }
}

@Module @InstallIn(SingletonComponent::class)
interface ContestsModule { @Binds @IntoSet fun entry(e: ContestsEntry): FeatureEntry }
```
Binding the same route as one of the app's placeholders replaces it (`withoutReplacedPlaceholders()`): never edit `PlaceholderModule`. The feature replacing the last placeholder deletes `PlaceholderEntry` and its helpers. Nested screens go in a nested graph routed `destination.route` or under `<route>/...`, so the item stays selected and the content pane keeps tag `screen_<route>`.

| Symbol | What it is |
|--------|-----------|
| `TopLevelDestination(route, label, icon, order)` | One bottom-bar / rail item. `label` is a string resource (text and content description); `testTag` is `nav_<route>`. `@Immutable`. |
| `FeatureEntry` | `destination` plus `register(graph: NavGraphBuilder)`, which adds the feature's screens (at least `destination.route`). `module: OptionalModule?` defaults to `null` (always shown); an optional module's entry overrides it. |
| `Iterable<FeatureEntry>.inNavigationOrder()` | Ascending `order`, ties broken by route. The shell's only ordering. |
| `Iterable<FeatureEntry>.withoutDisabled(disabledModules)` | Drops entries whose `module` is disabled; the shell applies it to `UserSettings.disabledModules`. |
| `NavigationModule` | Hilt `@Multibinds` declaration of `Set<FeatureEntry>` (valid when empty). |

Orders in use: Home 0, Contests 10, Problemset 20, Profile 30, Settings 40.
