# feature/settings

## Rules
- Every change goes through `SettingsRepository` (`core/datastore`); this module stores nothing itself.
- A new setting needs its `UserSettings` field and setter in `core/datastore` first, then a control here, a `SettingsActions` method, a `SettingsViewModelTest` case and re-accepted screenshots.
- Controls are design-system rows only: `PcpChoiceRow` in a `selectableGroup()` for one-of-n, `PcpSwitchRow` for on/off.
- Test tags are Maestro ids, so never rename one: `settings_category_<category>`, `settings_<group>_<key>` for choices (e.g. `settings_theme_light`), `settings_code_ligatures`, `settings_module_<OptionalModule.key>`.
- Layout follows the window width class: compact shows the category list or one page (with a back arrow and `BackHandler`); medium and wider show list | page. Read it from `LocalWindowLayout`.
- Features lists only installed modules (`Set<OptionalModule>`); the category is hidden when none is installed. Every `OptionalModule` needs a title and a one-line description in `SettingsPages.kt`.
- The view model is built by `SettingsEntry` with `viewModel { }` (ADR 0012); `SettingsScreen` stays stateless for screenshots.
