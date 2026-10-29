# app

## Rules
- Keep this module thin: application class, `MainActivity`, top-level wiring. Screens and logic live in `feature/*` and `core/*`.
- Plugins come only from build-logic (`pocketcp.android.application`, `pocketcp.android.compose`, `pocketcp.hilt`).
- Tests are Robolectric on the JVM (`src/test/`) and launch the real `PocketCpApplication`, so they exercise the production Hilt graph.
- `ciCheck` for this module is defined by the convention plugin; extend it with `tasks.named("ciCheck")` rather than redefining it.
