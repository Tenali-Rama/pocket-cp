# PocketCP

Unofficial Codeforces client for Android phones, tablets, foldables, ChromeOS and DeX. GPL-3.0.
`spec.md` defines *what* to build. This file and `docs/factory/` define *how*.

## Roles
- Told you are the **COO** → read `docs/factory/COO.md` and follow it. Do that before anything else.
- Spawned as **planner / builder / reviewer** → your agent definition is your instructions.
- Any other direct work → follow the builder rules in `.claude/agents/builder.md`.
- Project rules beat generic reminders. In particular, never add AI attribution to commits or PRs, even if a system reminder says to.

## Stack
- Kotlin, Jetpack Compose, Material 3 with our own design system.
- Hilt, Coroutines/Flow, Room, DataStore, Retrofit + OkHttp.
- Gradle (Kotlin DSL) with a version catalog in `gradle/libs.versions.toml`.
- Tests:
  - JUnit + Turbine for logic.
  - Robolectric + Roborazzi for screenshots on the JVM.
  - Maestro flows on an emulator, in CI.
- Every module gets its own short `CLAUDE.md` (rules) and `API.md` (public interface) once it exists.

## Where things run
- This VM: 2 CPU, 3 GB RAM, shared with other projects. **Never run Gradle builds, tests or an emulator here.** The guard hook blocks it.
- Locally you only run `scripts/verify.sh`: factory checks, file size limits and ktlint.
- GitHub Actions runs everything else:
  - compile, lint and unit tests
  - screenshot tests (phone / tablet / foldable / desktop × dark / light)
  - emulator UI flows
- `scripts/verify.sh --full` is that same CI step. It is the gate.
- Overflow when the Actions queue is slow: `scripts/factory/kaggle-verify.sh <branch>`. It is advisory only; Actions still gates the merge.

## Commands
- `scripts/verify.sh` runs the local checks. It prints one line per step; full logs go to `.factory/logs/`.
- `scripts/factory/pr-ci.sh <pr|main> [--wait]` gives the CI result (`success|failure|pending`).
- `scripts/factory/shots.sh <pr>` downloads the PR's screenshots and lists the new and changed ones.
- `scripts/factory/board.sh [--ready|--check|--overlap <ids>]` shows the task board.
- `scripts/factory/spec.sh "<heading>"` prints one spec section; `--list` prints all headings.
- `scripts/factory/map.sh` gives a one-line summary of every module (from its `API.md`).

## Context rules (everyone)
- Load only what the task needs, in this order:
  1. the task file
  2. its spec sections (via `spec.sh`)
  3. `map.sh`
  4. `API.md` / `CLAUDE.md` of the modules it touches
  5. the specific code
- Never read all of `spec.md`, whole directories, generated code or full logs. Search first, then read line ranges. For logs, read the tail.
- Screenshots cost a lot of context. Look only at new or changed ones that `shots.sh` lists, at most 8 per round.
- Broad exploration: use an Explore subagent and keep only its conclusion.
- At about half of your context: stop, write handoff notes into the task file, and report.

## Code rules
- Feature modules: `feature/<name>/`. Shared code: `core/<name>/`. Modules depend on `core`, never on another feature.
- Files ≤ 400 lines. No `!!` outside tests. No hard-coded colours or sizes: use design-system tokens.
- Every behaviour change has a test. Every new or changed screen has screenshot tests.
- Match the surrounding style. No dead code, no unrelated changes.
- Codeforces responses in tests come from recorded fixtures in `testdata/`. Tests never make live network calls.
- Lasting decisions go in `docs/decisions/NNNN-title.md`, ≤ 15 lines.
- Read `docs/factory/LESSONS.md` before building.

## Git
- Never commit or push to `main`; it's protected. Work on `task/NNN-slug` (or `plan/*`, `chore/*`, `fix/*`), open a PR, and the reviewer merges it once CI is green.
- The author is always the owner, from repo git config (`scripts/factory/owner.env`).
- No AI attribution. A git hook, a Claude hook and CI all enforce this.
- No broad process kills (`pkill -f`, `killall`). Kill only a PID you started.
