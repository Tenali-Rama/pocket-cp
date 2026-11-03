# core/statement

## Rules
- This module renders the judge-agnostic `Statement` model with Compose. It does not read settings or depend on
  `core/datastore`; callers resolve preferences to plain values.
- Statement text uses `PocketCpTheme` tokens. Images use the shared network `OkHttpClient` through the public image
  loader factory, with Coil's disk cache; binary image requests do not go through `RequestQueue`.
- `math/` is the replacement slot for native TeX rendering. Keep the default renderer's source-text fallback stable
  so the native typesetter can replace only that package.
- Tests use Compose/Robolectric and recorded, in-memory content. Screenshot tests use the required device matrix.
