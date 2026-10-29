---
id: 000
title: Short imperative title
milestone: M0
status: todo
deps: []
touches: [feature/example/, settings.gradle.kts+]
spec: ["7.3 Problemset"]
---

## Goal
One or two sentences: what exists after this task that didn't before.

## Acceptance criteria
- [ ] Test `ProblemListViewModelTest` covers filtering and the offline/error path, and passes in CI
- [ ] Screenshots (ADR 0005 naming `<name>[_<variant>]_<device>_<dark|light>`, variant before device): `problemList_tablet_dark`: list | detail two-pane; `problemList_phone_dark`: single pane; `problemList_empty_phone_dark` and `problemList_error_phone_dark` for the states
- [ ] (each criterion is a CI test, a named screenshot with what it must show, or a Maestro flow step)

## Out of scope
- Things a builder might be tempted to do, but that belong to other tasks.

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
