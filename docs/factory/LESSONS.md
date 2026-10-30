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
- Rebase onto `origin/main` right before you mark the PR ready. ADR numbers: take the next one that is free on main and not claimed by an open PR, and renumber at every rebase if it has been taken (collisions on #9, #11, #12, #26).
- Waiting on CI and no run appears for your head commit? Check `gh pr view <n> --json mergeable`. `CONFLICTING` means GitHub starts no `pull_request` runs: rebase on `origin/main` now instead of waiting (#10, #12, #27 each lost 40+ minutes).
- Never run Gradle, tests or an emulator on the VM. Push and let CI run them. Locally, run only the narrow static check you're fixing.
- Local ktlint misses detekt rules that CI enforces: 120-character line limit, `LongParameterList`, `InjectDispatcher`. Keep lines ≤ 120 characters and check those rules before pushing (seen on 007, 015, 017, 018).
- Download CI logs by run id into a fresh, empty directory. Never read logs other runs left in a shared scratchpad; stale logs caused a misdiagnosis.
- Screenshot names follow ADR 0005: `<name>[_<variant>]_<device>_<dark|light>`, even when a task's criteria word it differently.
- Never share a Gradle `build/` dir between worktrees. Sharing the dependency cache (`~/.gradle/caches`) is fine.

## Rebase
Shared files and append-only (`+`) files may conflict when parallel PRs land. Resolve them mechanically, then let CI check the result:
- `gradle/libs.versions.toml`, `API.md`, `settings.gradle.kts+` and other append-only files: keep both sides' lines, sorted where the file is sorted.
- Screenshot goldens: never hand-merge them. Take main's copy, then accept the fresh CI output for your own screens only (`shots.sh`).
- ADR number taken on main: rename yours to the next free number, and fix references to it.
- Any other conflict is a real one. Resolve it carefully, and say so in the PR comment so the reviewer re-reviews it.
