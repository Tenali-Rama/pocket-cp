---
id: 029
title: Spoiler-hiding settings, and a settings flow that survives read errors
milestone: M2
status: todo
deps: []
touches: [core/datastore/src/main/kotlin/app/pocketcp/core/datastore/UserSettings.kt, core/datastore/src/main/kotlin/app/pocketcp/core/datastore/SettingsRepository.kt, core/datastore/src/main/kotlin/app/pocketcp/core/datastore/DataStoreSettingsRepository.kt, core/datastore/src/test/kotlin/app/pocketcp/core/datastore/SettingsRepositoryTest.kt, core/datastore/API.md, feature/settings/src/test/kotlin/app/pocketcp/feature/settings/FakeSettingsRepository.kt]
spec: ["7.3 Problemset", "15. Settings"]
---

## Goal
The user's choice to hide problem tags and ratings (spec §7.3) persists across launches. This task also fixes an M1 follow-up (#25): today the settings flow emits defaults once after an `IOException`, then stops sending later changes.

## Acceptance criteria
- [ ] `UserSettings.hideProblemTags: Boolean = false` and `hideProblemRatings: Boolean = false` are stored under the keys `hide_problem_tags` and `hide_problem_ratings`. They have the setters `setHideProblemTags(Boolean)` and `setHideProblemRatings(Boolean)`.
- [ ] After a read `IOException`, `settings` emits defaults and keeps collecting: a later successful write is still emitted. Non-IO errors still propagate. One possible approach is `retryWhen` on `IOException`, after emitting defaults once, with a bounded backoff (builder's choice); document it in KDoc.
- [ ] `SettingsRepositoryTest` (Turbine) covers:
  - each new field's default and its round-trip
  - a DataStore whose first read throws `IOException` emits defaults, then the value written afterwards
- [ ] `FakeSettingsRepository` in `feature/settings` implements the new setters, and no settings goldens change.
- [ ] `core/datastore/API.md` table updated.

## Out of scope
- The toggles' UI (in the problemset, 032). A Settings-screen section for them.

## Notes for next
<!-- builder fills in -->
