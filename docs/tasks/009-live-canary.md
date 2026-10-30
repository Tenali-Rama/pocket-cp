---
id: 009
title: liveCanary Gradle task against live Codeforces
milestone: M0
status: done
deps: [008]
touches: [core/codeforces/build.gradle.kts, core/codeforces/src/canary/, core/codeforces/CLAUDE.md, gradle/libs.versions.toml]
spec: ["18. Known Risks"]
---

## Goal
The `liveCanary` task checks our client and parsing assumptions against live Codeforces, so `canary.yml` opens an issue when Codeforces changes. Later parser modules add their own canary checks the same way.

## Acceptance criteria
- [x] `liveCanary` is defined in `core/codeforces/build.gradle.kts` (so `canary.yml`'s `grep liveCanary --include=*.kts` finds it) and runs tests from a separate `src/canary/` source set.
- [x] `ciCheck` and the unit test tasks do not compile or run `src/canary/` (no live calls in CI gates).
- [x] Canary checks: live `contest.list`, `problemset.problems` and `user.info?handles=tourist` decode with the 008 DTOs; `https://codeforces.com/problemset/problem/4/A` is fetched and contains a `div.problem-statement` element (Jsoup). All calls go through the request queue.
- [x] A failing check names the endpoint or selector that broke within the last 40 lines of output (what `canary.yml` puts in the issue). A Cloudflare challenge (HTTP 403/503 with a `cf-mitigated` header or a challenge-page body) is reported as `BLOCKED by Cloudflare: <url>`, which is distinct from `SELECTOR BROKEN: <selector> on <url>`. A test in `src/canary/` checks both messages against small inline samples.
- [x] Proof in the PR: a `gh workflow run canary --ref <branch>` run that passes (link).
- [x] `core/codeforces/CLAUDE.md` explains how to add a canary check.

## Out of scope
- The statement parser itself (M3).

## Notes for next
- `src/canary/` is the `testLive` source set of a `live` build type (not `canary`: that build type's main source set would claim `src/canary/`); the variant is enabled only when `liveCanary` is on the command line (`gradle.startParameter`), so no CI gate compiles it and the IDE doesn't index it.
- Only package `app.pocketcp.core.codeforces.canary` runs under `liveCanary`; add checks there with `LiveCodeforces.endpoint(...)` / `.page(url, selector)` (one shared `RequestQueue`).
- `LiveCodeforces` records each path's last raw response under the queue, because the queue turns a 503 into `NetworkError.Unavailable` and drops the headers needed to spot a Cloudflare challenge.
- M3 parser canaries can live here (Jsoup is a `testLiveImplementation` dependency) or copy the build-type pattern in their own module; canary.yml finds any `liveCanary` in a `.kts`.
