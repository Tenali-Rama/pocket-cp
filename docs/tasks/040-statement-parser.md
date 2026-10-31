---
id: 040
title: Isolated Codeforces statement parser in core/parser, with recorded pages
milestone: M3
status: todo
deps: [039]
touches: [core/parser/, settings.gradle.kts+, gradle/libs.versions.toml, testdata/codeforces/pages/, testdata/README.md, docs/decisions/]
spec: ["7.4 Problem View", "18. Known Risks", "4. Platform & Architecture"]
---

## Goal
Spec §4 and §18 want HTML parsing isolated, so it can be fixed quickly when Codeforces changes its markup. After this task, the pure-JVM module `core/parser` turns a Codeforces problem page into a `Statement` (039), and it fails only with its own typed errors. Recorded pages under `testdata/` pin its behaviour.

## Acceptance criteria
- [ ] New module `core/parser` (`pocketcp.jvm.library`, jsoup from the catalog, `api(project(":core:model"))`), with `CLAUDE.md` and `API.md`:
  - `CodeforcesStatementParser.parse(html: String, problem: ProblemId, language: StatementLanguage): Statement`
  - throws only `StatementParseError`, which is sealed: `Blocked` (a Cloudflare challenge page) or `UnknownMarkup(detail)` (no `.problem-statement`, or no title)
- [ ] Recorded pages in `testdata/codeforces/pages/contest/<contestId>/problem/<index>.<en|ru>.html`:
  - the debug problemset's problems `2276F`, `2274A` and `2273D`, in both languages
  - pages that show an image, a table, a list, an interactive problem, and old-style `<pre>` samples with `<br>`
  - one hand-written Cloudflare challenge page, marked as hand-written in the README, like `error.call-limit.json`

  Each page is trimmed to the `html`/`body` wrapper, `.problem-statement` and the sidebar's contest-materials box (scripts and the rest removed, as `testdata/README.md` describes). The README's table lists every file.
- [ ] `CodeforcesStatementParserTest` (fixtures via `core/testing`'s `Fixtures`) covers:
  - header: title without the `A. ` prefix; time limit in ms; memory in MB; standard I/O gives `null` files
  - section kinds come from the markup classes (`.input-specification`, `.output-specification`, `.note`, interaction), never from heading text, so the Russian pages map the same kinds
  - TeX: `$$$…$$$` gives `Inline.Math`, `$$$$$$…$$$$$$` gives `DisplayMath`, and entities are decoded inside TeX (`&lt;` becomes `<`)
  - inline styles from `tex-font-style-bf` / `-it` / `-tt`
  - lists, tables (header cells flagged), and images with absolute `https://` URLs (protocol-relative and relative `src` resolved)
  - samples in both markups (per-line `div.test-example-line` and `<pre>` with `<br>`), each as newline-joined text in order
  - materials: title, absolute URL and kind (`Tutorial` for tutorial/editorial or "Разбор" links, `Announcement` for announcement/"Анонс", else `Other`) (default — owner may change)
  - the challenge page throws `Blocked`; a contest page without a statement throws `UnknownMarkup`; every fixture truncated at its midpoint throws only a `StatementParseError` (or parses), never another exception
- [ ] A new ADR: the parser module boundary, the error types, and that a parse failure means "fall back to the WebView for that page" (spec §18).

## Out of scope
- Fetching pages, mapping errors to `JudgeError`, and the live canary: those are 041. Rendering, caching.
- Custom Test, contest-materials pages or Gym parsing (later milestones extend this module).

## Notes for next
- Record with `curl` from the builder VM, waiting ≥ 2 s between calls. If Cloudflare blocks the VM, stop and ask the owner to record the pages; never hand-write a page that pretends to be recorded.
<!-- builder fills in -->
