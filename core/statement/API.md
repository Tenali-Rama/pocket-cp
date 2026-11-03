# core/statement API

Native Compose rendering of the judge-agnostic `Statement` from `core/model`.

Package `app.pocketcp.core.statement`. Add with `implementation(project(":core:statement"))`; `Statement` and its
block types are exposed through the module's `api(project(":core:model"))` dependency.

| Symbol | What it does |
|---|---|
| `StatementContent(statement, textScale, maxWidth, onOpenLink, modifier, state, header)` | One centred, width-capped lazy column with a header, sections, all statement block types, samples and materials. `textScale` applies to statement text on top of the app/system scale. |
| `LocalMathRenderer` | Composition local whose `Inline` and `Display` composables render TeX. The default renderer displays the TeX source in the code font with a `math` content description. |
| `LocalStatementImageLoader` | Optional image-loader local used by `StatementContent`; callers provide the loader built by `statementImageLoader`. |
| `statementImageLoader(context, client)` | Builds Coil's disk-cached image loader using the shared `core/network` `OkHttpClient`. |

The caller supplies `maxWidth = Dp.Infinity` to fill its pane, or one of the reading-width tokens from
`PocketCpTheme.spacing`. It supplies resolved preferences as `textScale` and the width; this module has no settings
dependency.
