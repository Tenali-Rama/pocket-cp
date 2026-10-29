# core/testing

## Rules
- Test helpers only. Modules use it as `testImplementation(project(":core:testing"))`; never as a main dependency.
- Tests never touch the network: they serve recorded fixtures from the repo-root `testdata/` through `FixtureServer` (an `HttpTransport` for a real `RequestQueue`).
- Fixtures are recorded and trimmed as `testdata/README.md` says. Never hand-write a body that pretends to be recorded without saying so there.
- An unmapped request must fail the test loudly (`AssertionError` with its path); don't add catch-all routes.
- No Android, Hilt or Compose here unless a helper needs it; keep it plain Kotlin.
