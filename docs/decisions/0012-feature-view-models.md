# 0012 — Feature view models are built by their `FeatureEntry`

- **Status**: accepted
- **Context**: The first feature screen (021, Settings) needs a `ViewModel` scoped to its nav back stack entry. `hiltViewModel()` needs `androidx.hilt:hilt-navigation-compose`, a new dependency, while every `FeatureEntry` is already built by Hilt.
- **Decision**:
  - The entry `@Inject`s what the screen needs and, in `register`, creates the view model with lifecycle's `viewModel { FooViewModel(dep, …) }` (from navigation-compose's `api`). No `@HiltViewModel`.
  - The view model takes its dependencies as constructor parameters, so tests build it with fakes directly.
  - Screens are stateless (`state`, callbacks) under a thin route composable, so screenshots never need a view model.
- **Consequences**: No Hilt view-model factory or extra library. A view model that needs `SavedStateHandle` reads it from `CreationExtras` in the initializer (`createSavedStateHandle()`). Switching to `@HiltViewModel` later is a per-feature change.
