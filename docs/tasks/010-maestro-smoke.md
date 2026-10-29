---
id: 010
title: Maestro smoke flow for the app shell
milestone: M0
status: in-progress
deps: [006]
touches: [.maestro/]
spec: ["5. Design & Layout"]
---

## Goal
CI's `ui` job runs a Maestro flow on the phone and tablet emulators that launches the app and walks the shell.

## Acceptance criteria
- [ ] `.maestro/smoke.yaml`: launch with cleared state, assert `nav_home` is visible, screenshot `smoke_home`; tap `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` in turn, asserting each placeholder title and taking `smoke_<destination>`; press back and assert Home is selected.
- [ ] CI `ui (phone)` and `ui (tablet)` jobs are green and their artifacts contain the 5 screenshots each (bottom bar on phone, rail on tablet).
- [ ] The flow uses test tags only (no coordinates), so it works on both form factors.

## Out of scope
- Flows for features that don't exist yet. Changes to `app/` (task 006 provides the tags).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
