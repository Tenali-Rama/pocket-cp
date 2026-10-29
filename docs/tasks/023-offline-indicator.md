---
id: 023
title: Network monitor and offline indicator in the shell
milestone: M1
status: todo
deps: [020]
touches: [core/network/src/main/kotlin/app/pocketcp/core/network/NetworkMonitor.kt, core/network/src/main/kotlin/app/pocketcp/core/network/NetworkModule.kt, core/network/src/test/kotlin/app/pocketcp/core/network/NetworkMonitorTest.kt, core/network/API.md, app/src/main/kotlin/app/pocketcp/MainActivity.kt, app/src/main/kotlin/app/pocketcp/shell/PocketCpApp.kt, app/src/main/kotlin/app/pocketcp/shell/PocketCpShell.kt, app/src/main/kotlin/app/pocketcp/shell/OfflineBanner.kt, app/src/main/res/values/strings.xml, app/src/test/kotlin/app/pocketcp/shell/OfflineBannerTest.kt, app/src/test/screenshots/, app/build.gradle.kts+, app/API.md]
spec: ["14. Offline", "16. Non-Functional"]
---

## Goal
When the device has no validated internet connection, the shell shows a clear, slim offline banner (spec §14). It disappears as soon as the connection returns.

## Acceptance criteria
- [ ] `NetworkMonitor.isOnline: Flow<Boolean>` comes from `ConnectivityManager` callbacks (`INTERNET` + `VALIDATED`), with distinct values only. It is bound in `NetworkModule`.
- [ ] `NetworkMonitorTest` (Robolectric shadow `ConnectivityManager`) covers: the initial state, lose → `false`, regain → `true`, and duplicate callbacks deduplicated.
- [ ] The banner sits above the content and under the status bar, on every form factor. It does not cover the bar or the rail. It reads "Offline · showing saved data" (default wording — owner may change), uses design tokens only, and is a polite live region.
- [ ] `OfflineBannerTest`: the banner is absent when online, present when offline, and announced once per change.
- [ ] Screenshots:
  - `shell_offline_<device>_<dark|light>`: all 8
  - `shell_offline_font150_phone_dark`

  Existing `shell_*` goldens are unchanged.

## Out of scope
- Offline drafts (M6). Per-screen stale labels (each feature, from `Cached.error`).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
