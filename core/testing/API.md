# core/testing API

Test-only helpers: `Fixtures` reads recorded responses from the repo-root `testdata/`, and `FixtureServer` serves them to a `RequestQueue` so no test touches the network.

Package `app.pocketcp.core.testing`. Use with `testImplementation(project(":core:testing"))`.

## `Fixtures`
- `fun text(path: String): String`: UTF-8 text of `testdata/<path>`; `IllegalStateException` if missing.
- `val root: File`: the `testdata/` directory, found by walking up from the working directory.

## `FixtureServer : HttpTransport`
- `fun map(path: String, fixture: String, code: Int = 200): FixtureServer`: answer a path plus decoded query (e.g. `/api/user.info?handles=a;b`) with `testdata/<fixture>` and `code`. Chainable.
- `execute(request)`: the mapped fixture (`Content-Type` from the extension), or an `AssertionError` naming the unmapped path.
- `val requests: List<Request>`: every request received, in order.

Typical use: `RequestQueue(FixtureServer().map(...), scopeOnTestScheduler)` inside `runTest`.
