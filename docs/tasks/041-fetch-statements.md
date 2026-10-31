---
id: 041
title: Fetch statements through JudgeSource; fixture builds serve recorded pages; canary parses a live page
milestone: M3
status: todo
deps: [038, 040]
touches: [core/model/src/main/kotlin/app/pocketcp/core/model/JudgeSource.kt, core/model/API.md, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesPages.kt, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/CodeforcesModule.kt, core/codeforces/src/main/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesJudgeSource.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/CodeforcesPagesTest.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesJudgeSourceTest.kt, core/codeforces/src/test/kotlin/app/pocketcp/core/codeforces/judge/CodeforcesModuleTest.kt, core/codeforces/src/canary/kotlin/app/pocketcp/core/codeforces/canary/, core/codeforces/build.gradle.kts, core/codeforces/API.md, core/codeforces/CLAUDE.md, core/data/src/test/kotlin/app/pocketcp/core/data/TestHarness.kt, app/src/fixture/kotlin/app/pocketcp/judge/FixtureJudgeSource.kt, app/src/fixture/kotlin/app/pocketcp/judge/AssetTransport.kt, app/src/live/kotlin/app/pocketcp/judge/LiveJudgeSourceModule.kt, app/src/testDebug/kotlin/app/pocketcp/judge/FixtureJudgeSourceTest.kt, app/build.gradle.kts, app/API.md, feature/problemset/src/test/kotlin/app/pocketcp/feature/problemset/FixtureProblems.kt, feature/contests/src/test/kotlin/app/pocketcp/feature/contests/FixtureContests.kt, testdata/README.md]
spec: ["7.4 Problem View", "18. Known Risks", "16. Non-Functional"]
---

## Goal
After this task the data layer can ask any judge for a statement, `JudgeSource.statement(problem, language)`, and for the page's URL for the WebView fallback. Codeforces fetches the problem page through the shared `RequestQueue` and parses it with `core/parser`. Fixture builds (debug, benchmarks) answer from the recorded pages, and the nightly canary checks a live page still parses.

## Acceptance criteria
- [ ] `JudgeSource` gets two methods with no default body:
  - `suspend fun statement(problem: ProblemId, language: StatementLanguage): Statement`, which throws only `JudgeError`
  - `fun problemUrl(problem: ProblemId, language: StatementLanguage): String`

  Every implementation implements both: `CodeforcesJudgeSource`, `FixtureJudgeSource` and `core/data`'s test `FakeJudgeSource`. The fake gates `statement` with a `CompletableDeferred` like its other calls, and returns `https://judge.invalid/<display>?<lang>` as the URL.
- [ ] `CodeforcesPages(queue, baseUrl = BASE_URL)` (Hilt singleton from `CodeforcesModule`) has `suspend fun problem(problem, language): String`:
  - a GET of `/contest/<id>/problem/<index>?locale=<en|ru>`, or `/gym/<id>/…` when `id ≥ 100000` (default — owner may change), through the queue
  - non-2xx and transport failures map to `CodeforcesError` as `CodeforcesApi` does
- [ ] `CodeforcesJudgeSource(api, pages)`:
  - `statement` = fetch + `CodeforcesStatementParser.parse`
  - `StatementParseError` (`Blocked` or `UnknownMarkup`) becomes `JudgeError.Malformed` with it as the cause: the UI's signal to fall back to the WebView
  - `problemUrl` is the same URL as the fetch, built without I/O
- [ ] `CodeforcesPagesTest` and `CodeforcesJudgeSourceTest` (`FixtureServer`, recorded pages from 040) cover:
  - the request path and `locale` for both languages and for a gym id
  - a parsed statement's title and sample count
  - offline gives `JudgeError.Offline`; HTTP 503 gives `Unavailable`; the challenge page and a non-problem page give `Malformed`
- [ ] Construction sites of `CodeforcesJudgeSource` pass `pages`: `LiveJudgeSourceModule`, `FixtureJudgeSource`, and the test helpers `FixtureProblems.kt` and `FixtureContests.kt`. No other change in those files.
- [ ] Fixture builds serve pages:
  - `AssetTransport` maps a problem-page request to `contest/<id>/problem/<index>.<lang>.html` (`text/html`); a missing page is 404, which gives `Malformed`
  - `testdata/codeforces/pages` is an asset dir of exactly the build types that have `testdata/codeforces/api` (038)
  - `FixtureJudgeSourceTest` covers `2274A` in `en` and `ru`, and a missing page
  - `checkFixtureBinding` (038) also fails if the release APK contains `assets/contest/`
- [ ] Live canary: a new `@Test` fetches one stable problem page (`1A`, en and ru) through `LiveCodeforces` and parses it with the real parser. A parse failure fails with `PAGE BROKEN: <url>: <detail>`, a challenge with `BLOCKED by Cloudflare: <url>`. `CanaryReportTest` covers both messages offline.
- [ ] `core/model/API.md`, `core/codeforces/API.md` + `CLAUDE.md` (pages are a separate class from the API client), `app/API.md` and `testdata/README.md` are updated.

## Out of scope
- Caching statements (043), the problem view (046), the WebView (047).
- Spacing HTML page requests in the queue: `core/network` stays unchanged, and 043's prefetch is sequential instead.

## Notes for next
<!-- builder fills in -->
