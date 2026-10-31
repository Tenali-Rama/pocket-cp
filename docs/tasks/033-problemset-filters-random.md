---
id: 033
title: Problemset filters (tags, rating range, division) and random problem
milestone: M2
status: done
deps: [028, 032]
touches: [feature/problemset/, .maestro/problemset.yaml]
spec: ["7.3 Problemset", "5. Design & Layout"]
---

## Goal
The problemset can be narrowed by tags, rating range and contest division, and the user can pick a random problem that matches the current filters.

## Acceptance criteria
- [ ] The pure `ProblemFilter`, unit tested in `ProblemFilterTest`, applies these rules:
  - tags: every selected tag must match (AND, as on Codeforces; default — owner may change)
  - rating range: 800–3500 in steps of 100; unrated problems are excluded once the range is narrowed
  - division: multi-select over `ContestDivision`, looked up from `ContestRepository.contests(gym = false)` by `contestId`; a problem whose contest isn't cached is kept only when no division is selected
  - all of these combine with 032's search
- [ ] Filter UI: a bottom sheet on compact and medium widths, and a side panel next to the list on expanded widths (default — owner may change). An active-filter count shows on the filter button, and "Clear" resets everything. The filters survive process death.
- [ ] While "Hide tags" is on, the tag filter stays usable but is collapsed behind a "Show tag filter" control (default — owner may change).
- [ ] "Random" (tag `problemset_random`) picks uniformly from the filtered list with an injected `Random`, scrolls to that row and highlights it (`ProblemRow(highlighted = true)`). It is disabled when the list is empty.
- [ ] `ProblemsetViewModelTest` adds: each filter, the combination with search, division lookup with a missing contest, a seeded random pick, random disabled when empty, and restored filters.
- [ ] Screenshots:
  - `problemsetFilters_phone_<dark|light>`: the sheet open with 2 tags and the 1200–1600 range
  - `problemsetFilters_<tablet|desktop>_<dark|light>`: side panel and list
  - `problemsetFilters_foldable_<dark|light>`
  - `problemset_random_phone_dark`: highlighted row
  - `problemsetFilters_font150_phone_dark`
- [ ] Maestro `problemset.yaml` adds: open the filters, select a fixture tag, apply, tap `problemset_random`, assert a highlighted `problem_*` row is visible, take screenshot `problemset_random`.

## Out of scope
- Solved/unsolved filter (needs the account, M4). Saved filter presets (not in spec).

## Notes for next
- Defaults taken: tags AND; active count = selected tags + selected divisions + 1 for a narrowed rating range; filters apply live (the sheet's "Done" just closes it); the rating filter stays usable while ratings are hidden.
- The sheet is `FilterSheet`, drawn in the window over a scrim (Back, scrim tap or "Done" close it), not M3 `ModalBottomSheet`: a dialog window is neither captured by Roborazzi nor tappable by id in Maestro.
- Expanded width is three panes: search/sort/Random | list | `FilterPanel`. Medium keeps search | list and opens the sheet.
- Divisions come from `ContestRepository.contests(gym = false)`; collecting it can start a (non-forced) contest refresh, like the Contests tab. The debug fixtures share no contest ids with the problemset, so the Maestro flow filters by tag.
- `problemsetFilters_*` screenshots use a 9-problem in-test sample (`FilterSample`), since the 3-problem fixture has nothing in 1200–1600.
