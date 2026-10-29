---
id: 012
title: Static check for module docs and feature isolation
milestone: M0
status: todo
deps: []
touches: [scripts/factory/check-modules.sh, scripts/factory/test-check-modules.sh, scripts/verify.sh]
spec: ["4. Platform & Architecture"]
---

## Goal
`scripts/verify.sh` (local and CI) fails when a Gradle module lacks `CLAUDE.md` or `API.md`, or when a `feature/` module depends on another feature.

## Acceptance criteria
- [ ] `scripts/factory/check-modules.sh`: every dir with a `build.gradle.kts` other than the repo root (incl. `build-logic/`) has `CLAUDE.md` and `API.md`; prints `missing API.md: <dir>/` per gap and exits 1.
- [ ] The same script fails with `feature dependency: feature/a -> feature/b` when a `feature/*/build.gradle.kts` references another feature project (`:feature:` or `projects.feature`).
- [ ] `scripts/factory/test-check-modules.sh` builds temp fixture trees covering the pass, missing-docs and feature→feature cases; `verify.sh` runs both as steps `modules` and `modules-test`.
- [ ] `scripts/verify.sh` passes on the current repo (no modules yet) and runs no Gradle.

## Out of scope
- Gradle-side dependency rules.

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
