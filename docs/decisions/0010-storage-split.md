# 0010 — What goes in DataStore, Room and the encrypted store

- **Status**: accepted
- **Context**: Spec §4 names three stores. Without a rule, settings drift into Room, caches into DataStore, or secrets into plain files.
- **Decision**:
  - Preferences DataStore (`core/datastore`, one `settings` file): small user choices that are read as a whole and restored with the export (spec §15). No lists that grow, no secrets.
  - Room (`core/database`): anything queried, growing or user-authored: the API cache, code files, notes, snippets, practice data.
  - Encrypted store (EncryptedSharedPreferences/Keystore): session cookies, the Codeforces API key and AI keys. A setting that is a secret (e.g. an AI key in §15) lives there, even though its screen is Settings.
  - Settings enums are stored by a stable `key` string, never by name or ordinal, and unknown values read as the default.
- **Consequences**: Renaming an enum entry or reordering it never breaks stored settings. A secret never reaches DataStore or an export of it. A setting that starts to grow (e.g. per-language templates) moves to Room.
