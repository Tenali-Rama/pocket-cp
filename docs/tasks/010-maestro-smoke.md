---
id: 010
title: Maestro smoke flow for the app shell
milestone: M0
status: done
deps: [006]
touches: [.maestro/]
spec: ["5. Design & Layout"]
---

## Goal
CI's `ui` job runs a Maestro flow on the phone and tablet emulators that launches the app and walks the shell.

## Acceptance criteria
- [x] `.maestro/smoke.yaml`: launch with cleared state, assert `nav_home` is visible, screenshot `smoke_home`; tap `nav_contests`, `nav_problemset`, `nav_profile`, `nav_settings` in turn, asserting each placeholder title and taking `smoke_<destination>`; press back and assert Home is selected.
- [x] CI `ui (phone)` and `ui (tablet)` jobs are green and their artifacts contain the 5 screenshots each (bottom bar on phone, rail on tablet).
- [x] The flow uses test tags only (no coordinates), so it works on both form factors.

## Out of scope
- Flows for features that don't exist yet. Changes to `app/` (task 006 provides the tags).

## Notes for next
- `.maestro/smoke.yaml` (appId `app.pocketcp`) runs on `ui (phone)` and `ui (tablet)`; `maestro test .maestro/` picks up every flow in the directory.
- Select by test tag (`id: nav_*`); tab state is checked with `selected: true`, which Compose exposes for nav items.
- A placeholder title repeats its nav label, so the flow asserts `text: <Label>, index: 1` (a second node besides the nav item). A feature that replaces a placeholder must update its step in the flow.
- `takeScreenshot: <name>` lands in the `ui-<device>` artifact as `<run>/Shell smoke/takeScreenshot/<name>.png` (Maestro bundles it into `--test-output-dir`).
- The emulator runs in the system default (light) theme; the flow does not switch to dark.
