---
id: 048
title: Problemset opens statements, list | statement on wide windows
milestone: M3
status: todo
deps: [046]
touches: [feature/problemset/, .maestro/problemset.yaml]
spec: ["7.3 Problemset", "7.4 Problem View", "5. Design & Layout"]
---

## Goal
Problem rows in the Problemset tab finally open something. After this task, a tap shows the statement: as a page on phones, and beside the list on medium and wider windows (spec §5: list–detail panes). The tab also registers `problemset/{id}`, so later cross-links and deep links (M4) can open a problem.

## Acceptance criteria
- [ ] The opened problem lives in `ProblemsetViewModel`'s `SavedStateHandle` as its `display` (ADR 0016). `ProblemsetActions` gains `open(ProblemId)` and `close()`. Rows get `onClick = { open(it.id) }` and show `highlighted`/`selected` for the opened one on wide windows.
- [ ] Layout:
  - Compact: the opened problem replaces the list with `ProblemPane(onBack = close)`; `BackHandler` closes it; scroll position and filters are kept.
  - Medium and expanded: list | statement, statement pane ≥ 55 % of the width.
  - With none opened, the first problem of the current filtered list is shown (ADR 0016 default); with an empty list, an `EmptyState` "Select a problem".
  - On expanded, the filter controls move to the Filters sheet while a statement is shown (default — owner may change).
- [ ] `ProblemsetEntry` injects `ProblemViewModelFactory` and also registers `problemset/{id}` (`id` such as `2274A`): the same screen, seeded once; a saved selection wins afterwards (ADR 0016).
- [ ] `ProblemsetViewModelTest`:
  - `open`/`close` and the persisted selection
  - the default shown problem follows the filtered list
  - the seeded route id is applied once and not after `close()`
  - an id that isn't in the cache still opens (the statement loads by id)
- [ ] Screenshots: `problemsetDetail_<device>_<theme>`:
  - phone: the statement page with a back arrow
  - tablet/foldable/desktop: list | statement with the opened row highlighted
  - also `problemsetDetail_default_tablet_dark` (none opened: first problem) and `problemsetDetail_empty_tablet_dark`
  - existing `problemset_*` goldens that now show a statement pane are re-accepted
- [ ] `.maestro/problemset.yaml` adds:
  - tap `problem_2274A`
  - `problem_view` visible
  - tap `sample_1_input_copy`
  - back
  - `problem_2274A` visible again (fixture data, ADR 0015)

## Out of scope
- Random pick opening the statement, bookmark filters, and any change to `core/problemview`.

## Notes for next
<!-- builder fills in -->
