---
id: 032
title: Problemset feature with search by name or ID, sort and spoiler hiding
milestone: M2
status: todo
deps: [025, 026, 027, 029]
touches: [feature/problemset/, settings.gradle.kts+, app/build.gradle.kts+, .maestro/problemset.yaml]
spec: ["7.3 Problemset", "2. Product Principles", "3. Performance Budgets"]
---

## Goal
The Problemset tab is a native, cache-first list of the whole Codeforces problemset. It can be searched by name or ID (`1900A`), sorted, and shown without tags or ratings. It replaces the `problemset` placeholder by route (025).

## Acceptance criteria
- [ ] `feature/problemset` binds the `problemset` `FeatureEntry` (order 20, same label and icon), with its view model per ADR 0012 over `ProblemRepository` and `SettingsRepository`.
- [ ] Search uses a pure `ProblemQuery` function, unit tested in `ProblemQueryTest`:
  - the query is trimmed and case-insensitive
  - an ID-shaped query (`^\d+[A-Za-z]\d*$` or `^\d+$`) matches by `display` prefix, so `1900` gives every problem of contest 1900 and `1900a` matches `1900A`
  - anything else is a substring match on the name
- [ ] Sort options: newest (default), rating ascending, rating descending, most solved, name. Problems without a rating sort last in both rating orders.
- [ ] Two toggles, "Hide tags" and "Hide ratings", sit in the top bar's overflow menu. They write through `SettingsRepository` and are applied to every row. With hidden ratings, the rating sorts are unavailable (default — owner may change).
- [ ] Filtering and sorting of the roughly 10k problems run off the main thread (`Dispatchers.Default`, injected). The query and sort survive process death.
- [ ] Data states as in 030: no spinner over data, banner on a stale error, full error, loading and empty (no match).
- [ ] `ProblemsetViewModelTest` covers search, each sort, the toggles written and applied, restored state and the data states.
- [ ] Screenshots (fixture `problemset.problems.json`):
  - `problemset_<device>_<dark|light>`: all 8
  - `problemset_search_phone_dark`: query `1900` showing only that contest
  - `problemset_hiddenSpoilers_phone_dark`
  - `problemset_empty_phone_dark`, `problemset_error_phone_dark`
  - `problemset_font150_phone_dark`
- [ ] Maestro `.maestro/problemset.yaml` (debug fixtures):
  - tap `nav_problemset` and assert `screen_problemset`
  - type a fixture problem id into the search field (tag `problemset_search`) and assert `problem_<id>`
  - turn on "Hide tags", relaunch without clearing state, and assert a known tag text is not visible
  - take screenshot `problemset_search`
- [ ] `feature/problemset/CLAUDE.md` and `API.md`.

## Out of scope
- Tag/rating/division filters and random (033). Status badges and solved/unsolved (M4). Opening a problem (M3).

## Notes for next
<!-- builder fills in -->
