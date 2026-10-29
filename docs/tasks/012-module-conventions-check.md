---
id: 012
title: Static check for module docs and feature isolation
milestone: M0
status: done
deps: []
touches: [scripts/factory/check-modules.sh, scripts/factory/test-check-modules.sh, scripts/verify.sh]
spec: ["4. Platform & Architecture"]
---

## Goal
`scripts/verify.sh` (local and CI) fails when a Gradle module lacks `CLAUDE.md` or `API.md`, or when a `feature/` module depends on another feature.

## Acceptance criteria
- [ ] `scripts/factory/check-modules.sh`: the module set is every project named in an `include(":a:b")` / `include(":a", ":b")` call in root `settings.gradle.kts` (`:a:b` → `a/b/`), plus `build-logic/` if it exists (one included build; its sub-project `build-logic/convention/` is *not* a separate module). Each must have `CLAUDE.md` and `API.md`; prints `missing API.md: <dir>/` per gap and exits 1. No root `settings.gradle.kts` → empty set → pass.
- [ ] The same script fails with `feature dependency: feature/a -> feature/b` when a `feature/*/build.gradle.kts` references another feature project (`:feature:` or `projects.feature`).
- [ ] `scripts/factory/test-check-modules.sh` builds temp fixture trees covering: pass; missing docs; feature→feature; no `settings.gradle.kts`; `build-logic/convention/build.gradle.kts` with docs only at `build-logic/` (pass); a multi-argument `include(...)`; `verify.sh` runs both as steps `modules` and `modules-test`.
- [ ] `scripts/verify.sh` passes on `main` at merge time (with whichever M0 tasks have landed by then) and runs no Gradle.

## Out of scope
- Gradle-side dependency rules.

## Notes for next
- Any module added to `settings.gradle.kts` needs `CLAUDE.md` + `API.md` in the same PR, or `verify.sh` fails (`modules` step).
- `build-logic/` counts as one module once the directory exists; its `convention/` sub-project needs no docs of its own.
- Feature isolation is matched textually on `:feature:<x>` and `projects.feature.<x>` in `feature/*/build.gradle.kts` (line comments ignored); Gradle-side rules are still out of scope.
- `check-modules.sh [root]` takes an optional root dir, which the fixture test uses.
