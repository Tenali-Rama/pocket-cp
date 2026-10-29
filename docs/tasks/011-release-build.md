---
id: 011
title: Release APK build with R8 and optional signing
milestone: M0
status: done
deps: [001]
touches: [app/build.gradle.kts, app/proguard-rules.pro, app/CLAUDE.md, docs/decisions/]
spec: ["17. Play Store Compliance", "1. Product"]
---

## Goal
The app's release build produces a minified APK, signed when `release.yml` passes a keystore, so each milestone tag yields an APK the owner can install.

## Acceptance criteria
- [ ] Release build type: R8 minify + resource shrinking, `app/proguard-rules.pro`.
- [ ] `-Pkeystore=<path>` plus env `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (exactly what `release.yml` passes) configures the release signing config; without `-Pkeystore`, configuration and the debug build don't fail.
- [ ] The app module's `ciCheck` also builds the (unsigned) release APK, so R8 problems fail CI; CI `verify` green.
- [ ] `versionName` / `versionCode` are defined in one place in `app/build.gradle.kts`, starting at `0.0.0` / `1`. Scheme: closing milestone n sets `versionName = "0.<n>.0"` and `versionCode = <n> + 1`, in the last PR of the milestone and before the `v0.<n>.0` tag (default — owner may change). `app/CLAUDE.md` states this rule.
- [ ] ADR: signing, versioning and the debug-APK fallback in `release.yml`.

## Out of scope
- Play upload, App Bundle, Baseline Profiles (M1/M14).

## Notes for next
- Version lives in `appVersionName` / `appVersionCode` at the top of `app/build.gradle.kts`; the last PR of milestone n bumps it to `0.<n>.0` / `<n> + 1` (rule in `app/CLAUDE.md`).
- The app's `ciCheck` runs `assembleRelease` (R8 + resource shrinking), so a missing keep rule fails the PR. Add rules to `app/proguard-rules.pro` with a reason.
- Code using reflection (e.g. kotlinx.serialization, Retrofit interfaces) must ship consumer rules or add keep rules here.
- Release signing exists only with `-Pkeystore=<path>` + env; the signed path is first exercised by a tag push with the `KEYSTORE_B64` secret set.
- ADR 0004 records signing, versioning and the debug-APK fallback.
