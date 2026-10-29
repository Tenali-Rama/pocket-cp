---
id: 011
title: Release APK build with R8 and optional signing
milestone: M0
status: todo
deps: [001]
touches: [app/build.gradle.kts, app/proguard-rules.pro, docs/decisions/]
spec: ["17. Play Store Compliance", "1. Product"]
---

## Goal
The app's release build produces a minified APK, signed when `release.yml` passes a keystore, so each milestone tag yields an APK the owner can install.

## Acceptance criteria
- [ ] Release build type: R8 minify + resource shrinking, `app/proguard-rules.pro`.
- [ ] `-Pkeystore=<path>` plus env `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (exactly what `release.yml` passes) configures the release signing config; without `-Pkeystore`, configuration and the debug build don't fail.
- [ ] The app module's `ciCheck` also builds the (unsigned) release APK, so R8 problems fail CI; CI `verify` green.
- [ ] `versionName` / `versionCode` defined in one place in `app/build.gradle.kts`.
- [ ] ADR: signing, versioning and the debug-APK fallback in `release.yml`.

## Out of scope
- Play upload, App Bundle, Baseline Profiles (M1/M14).

## Notes for next
<!-- builder fills in: ≤ 5 bullets that a later task needs to know -->
