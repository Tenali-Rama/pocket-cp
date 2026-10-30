# 0016 — List | detail inside one destination; `<route>/{id}` opens it preselected

- **Status**: accepted
- **Context**: Contest detail (031) needs list → detail on phones and list | detail on wide windows, but `FeatureEntry.register(NavGraphBuilder)` gives a feature no `NavController` to navigate with, and the Settings pattern (021) already keeps its open page as screen state.
- **Decision**:
  - The opened item is view-model state in `SavedStateHandle`, not a back stack entry. Compact width shows its page with a back arrow and `BackHandler` that clear it; medium and wider show list | detail, with a default item when none is opened.
  - The feature also registers `<route>/{id}` (e.g. `contests/{id}`): the same screen, its view model seeded from the `id` argument once; a saved selection (including "closed") wins afterwards.
- **Consequences**: No change to `core/navigation` or the shell; selection survives rotation, resize and process death with the screen. Cross-feature links and deep links (M4) navigate to `<route>/{id}`. Back on a phone's detail page closes it inside the tab; it never pops the nav stack.
