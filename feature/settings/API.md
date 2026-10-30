# feature/settings API

The Settings tab: Appearance (theme, text sizes, ligatures, density), Features (turn installed optional modules off) and About (name, version, unofficial-client disclaimer, licence), as list | page on wide windows and list → page on phones.

Package `app.pocketcp.feature.settings`. The app adds it with `implementation(project(":feature:settings"))`; nothing else depends on it.

| Symbol | What it is |
|--------|-----------|
| `SettingsEntry` | The `FeatureEntry`: route `settings`, label `R.string.settings_label` ("Settings"), `Icons.Filled.Settings`, order 40, tag `nav_settings`. Bound `@IntoSet` by `SettingsModule`. Injects `SettingsRepository` and `Set<OptionalModule>`. |
| `SettingsViewModel(repository, installedModules)` | `state: StateFlow<SettingsUiState?>` (`null` until the first read); implements `SettingsActions`, each setter writes through the repository. |
| `SettingsUiState(settings, installedModules)` | `categories`: `Appearance`, `Features` when a module is installed, then `About` (always last). |
| `SettingsActions` | `setTheme`, `setAppTextSize`, `setCodeTextSize`, `setCodeLigatures`, `setDensity`, `setModuleEnabled(module, enabled)`. |
| `SettingsCategory` | `Appearance`, `Features`, `About`. |

Test tags (Maestro ids): `settings_category_appearance`, `settings_category_features`, `settings_category_about`, `settings_theme_<system|dark|light|amoled>`,
`settings_app_text_size_<key>`, `settings_code_text_size_<key>`, `settings_code_ligatures`, `settings_density_<comfortable|compact>`,
`settings_module_<OptionalModule.key>`, `settings_about_<name|version|disclaimer|licence>`.

Screenshots: `settings_<device>_<theme>`, `settings_appearance_phone_<theme>`, `settings_features_tablet_<theme>`,
`settings_appearance_font150_phone_dark`, `settings_loading_phone_dark`, `about_<device>_<theme>`, `about_font150_phone_dark`. Maestro: `.maestro/settings.yaml`.
