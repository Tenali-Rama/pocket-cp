# feature/home API

Home, the start destination: a guest card, then running contests ("Ends in") and the next 5 upcoming ones ("Starts in") with live countdowns, side by side on expanded windows.

Package `app.pocketcp.feature.home`. The app adds it with `implementation(project(":feature:home"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `HomeEntry` | The `FeatureEntry`: route `home`, label `R.string.home_label` ("Home"), `Icons.Filled.Home`, order 0 (start destination), tag `nav_home`. Replaces the `home` placeholder. Bound `@IntoSet` by `HomeModule`. Injects `ContestRepository`. |
| `HomeViewModel(repository, clock, computeDispatcher = Default)` | `state: StateFlow<HomeUiState>` over the bounded `activeContests(upcomingLimit = 5)`; never the full `contests(...)` list. When a countdown reaches zero it re-sections locally and calls `refresh(gym = false, force = true)`. Implements `HomeActions`. |
| `HomeActions` | `refresh()`: the user's refresh (button, Retry), always forced; shows the pull indicator while it runs. |
| `HomeUiState(status, groups, error, userRefreshing)` | `status`: `Loading`, `Error`, `Empty` (no running or upcoming contest), `Loaded`. `groups`: non-empty `HomeGroup(section, contests)`, `Running` (ending soonest first) then `Upcoming` (soonest first, at most 5). |

Test tags (Maestro ids): `home_guest`; rows are `core/ui`'s `contest_<id>`; the shell tags the pane `screen_home`.

Screenshots: `home_<device>_<theme>`, `home_noContests_phone_dark`, `home_error_phone_dark`, `home_staleError_phone_dark`,
`home_loading_phone_dark`, `home_font150_phone_dark`. Maestro: `.maestro/home.yaml`.
