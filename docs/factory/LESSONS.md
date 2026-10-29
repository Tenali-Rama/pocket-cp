# Lessons

These rules come from repeated review findings, and the COO maintains them through retro PRs. Every builder reads this file before starting.

- Keep this file to at most 30 bullets. Merge or drop stale ones.

<!-- rules go below. The first ones are carried over from the polykov factory. -->
- `touches` must name every file the task edits, hubs included:
  - `settings.gradle.kts`
  - the module `build.gradle.kts`
  - `gradle/libs.versions.toml`
  - the nav graph
  - the DI module
  - ADRs

  A missing hub entry makes parallel dispatch unsafe. It was polykov's most common review note.
- Put a new feature in its own module (`feature/<name>/`) with its own tests. Shared hubs get only append-only registration lines (a `touches` entry with a trailing `+`).
- Rebase onto `origin/main` right before you mark the PR ready, and pick your ADR number at that moment. Picking it earlier caused duplicate ADR numbers.
- Never run Gradle, tests or an emulator on the VM. Push and let CI run them. Locally, run only the narrow static check you're fixing.
- Never share a Gradle `build/` dir between worktrees. Sharing the dependency cache (`~/.gradle/caches`) is fine.

## Rebase
Shared files and append-only (`+`) files may conflict when parallel PRs land. Resolve them mechanically, then let CI check the result:
- `gradle/libs.versions.toml`, `API.md`, `settings.gradle.kts+` and other append-only files: keep both sides' lines, sorted where the file is sorted.
- Screenshot goldens: never hand-merge them. Take main's copy, then accept the fresh CI output for your own screens only (`shots.sh`).
- ADR number taken on main: rename yours to the next free number, and fix references to it.
- Any other conflict is a real one. Resolve it carefully, and say so in the PR comment so the reviewer re-reviews it.
