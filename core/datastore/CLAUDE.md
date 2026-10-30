# core/datastore

## Rules
- Only user settings live here (ADR 0010). Cached or user-authored data goes to Room (`core/database`); sessions and keys go to the encrypted store, never here.
- One Preferences DataStore, `settings`, opened once by `DataStoreModule` (DataStore allows one instance per file). Never open another on it; inject `SettingsRepository`.
- Persisted names are forever: a preference name in `DataStoreSettingsRepository.Keys`, a `StoredPreference.key` and an `OptionalModule.key` are never renamed or reused. Enums are stored by `key`, never by `name` or ordinal.
- Reads never fail: an unknown stored value maps to the field's default, an IO error to the defaults, and a corrupted file is replaced (`ReplaceFileCorruptionHandler`). Keep that when adding a field.
- A new setting is a `UserSettings` field with a default, one preference name, one setter, and a round-trip test. Add settings with the task that needs them, not ahead of time.
- Tests: `settingsDataStore` on a `TemporaryFolder` file in the test's `backgroundScope`, Turbine for re-emission. No Robolectric except for the Hilt module test.
