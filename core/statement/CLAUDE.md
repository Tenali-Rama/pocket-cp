# core/statement

## Rules
- This module renders the judge-agnostic `Statement` model with Compose. It does not read settings or depend on
  `core/datastore`; callers resolve preferences to plain values.
- Statement text uses `PocketCpTheme` tokens. Images use the shared network `OkHttpClient` through the public image
  loader factory, with Coil's disk cache; binary image requests do not go through `RequestQueue`.
- `math/` contains the native TeX parser/layout and renderer. Keep its source-text fallback stable for malformed or
  unsupported input; do not introduce a WebView or network-backed typesetter.
- Tests use Compose/Robolectric and recorded, in-memory content. Screenshot tests use the required device matrix.
