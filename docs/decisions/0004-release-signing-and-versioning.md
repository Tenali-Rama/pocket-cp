# 0004 — Release signing, versioning and the debug-APK fallback

- **Status**: accepted
- **Context**: Each milestone tag should give the owner an installable APK. The keystore must never live in the repo, and CI must catch R8 breakage before a tag does.
- **Decision**:
  - The release build type uses R8 minify and resource shrinking. The app's `ciCheck` builds the unsigned release APK on every PR.
  - `-Pkeystore=<path>` plus env `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` creates the release signing config. Without `-Pkeystore` there is none, and the release APK is unsigned.
  - `release.yml` decodes the `KEYSTORE_B64` secret and builds the signed release APK. If that secret is missing it publishes a debug-signed APK (`-debug.apk`) so a tag still yields something to sideload.
  - Version lives only in `app/build.gradle.kts`, starting at `0.0.0` / `1`. Closing milestone n sets `0.<n>.0` / `<n> + 1` in its last PR, before the `v0.<n>.0` tag.
- **Consequences**: Debug and release APKs have different signatures, so switching between them needs an uninstall. Don't hard-code signing values or make configuration require a keystore. Losing the keystore means a new app identity for sideloaded installs.
