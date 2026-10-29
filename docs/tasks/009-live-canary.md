---
id: 009
title: liveCanary Gradle task against live Codeforces
milestone: M0
status: todo
deps: [008]
touches: [core/codeforces/build.gradle.kts, core/codeforces/src/canary/, core/codeforces/CLAUDE.md, gradle/libs.versions.toml]
spec: ["18. Known Risks"]
---

## Goal
The `liveCanary` task checks our client and parsing assumptions against live Codeforces, so `canary.yml` opens an issue when Codeforces changes. Later parser modules add their own canary checks the same way.

## Acceptance criteria
- [ ] `liveCanary` is defined in `core/codeforces/build.gradle.kts` (so `canary.yml`'s `grep liveCanary --include=*.kts` finds it) and runs tests from a separate `src/canary/` source set.
- [ ] `ciCheck` and the unit test tasks do not compile or run `src/canary/` (no live calls in CI gates).
- [ ] Canary checks: live `contest.list`, `problemset.problems` and `user.info?handles=tourist` decode with the 008 DTOs; `https://codeforces.com/problemset/problem/4/A` is fetched and contains a `div.problem-statement` element (Jsoup). All calls go through the request queue.
- [ ] A failing check names the endpoint or selector that broke within the last 40 lines of output (what `canary.yml` puts in the issue).
- [ ] Proof in the PR: a `gh workflow run canary --ref <branch>` run that passes (link).
- [ ] `core/codeforces/CLAUDE.md` explains how to add a canary check.

## Out of scope
- The statement parser itself (M3).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
