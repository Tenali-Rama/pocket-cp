# Running the factory on Omnirush

The factory's process, gates and rules don't change. This file maps the Claude-specific parts to Omnirush.

## The sandbox
Every session runs in a Linux box kept for this folder: `HOME` is `/root`, and only this project folder is shared with the machine.
- Tools live in the box. If `gh`, `ktlint` or `gitleaks` is missing, or preflight fails on them, run `scripts/factory/box-setup.sh`.
- Environment variables don't reach the box. `gh` and `git` get the GitHub token from `.factory/gh_token` through the wrapper and credential helper that `box-setup.sh` installs.
- Keep all work under this folder. Worktrees go in `.factory/worktrees/`.
- There is no `omnirush` command in the box, so agents are started with the `subagent` tool.

## Agents
| In `COO.md` | Here |
|---|---|
| `planner`, `builder`, `reviewer` agent types | a `general` subagent, in the background, with the prompt below |
| `Explore` | an `explore` subagent |
| `isolation: "worktree"` | the agent creates its own worktree (below) |
| `SendMessage` to resume an agent | continue the subagent with its `sessionID` |
| "All agents run on Opus" | all agents use this session's model |

Start every agent with this, then the usual `COO.md` brief:

> You are the PocketCP `<role>`. Read `AGENTS.md`, then `.claude/agents/<role>.md`, and follow them.

Builders also get this:

> Before any work: `git fetch origin && git worktree add .factory/worktrees/<task-id> -b <branch> origin/main`, or without `-b` when the branch exists. Run every command from that folder, using `cd <folder> && ...`, because the working directory may not persist between commands. Remove the worktree after the PR merges.

Planners and reviewers use `.factory/worktrees/plan-<milestone>` and `.factory/worktrees/review-<pr>` the same way (reviewers in detached mode), and never check out anything in the main checkout.

## Token rules
Every model call re-sends the agent's whole context, so one step in a 200k context costs far more than one in a 50k context. These rules override the waiting and CI steps in `builder.md`. Don't weaken any quality gate to save tokens.

**The watcher.** `scripts/factory/watch.py` runs outside the sandbox and does its waiting without using tokens. It messages the COO when an agent passes 100k context, when an agent stops on an error, and when CI finishes for an open PR. Start it next to the COO, from outside the sandbox:

```
tmux new -d -s coo-watch -c ~/projects/pocketcp scripts/factory/watch.py
```

Keep typed messages to the COO short while it runs: its messages are pasted into the same input box.

**Builders.**
- Never wait for CI: no `pr-ci.sh --wait`, `sleep` or repeated checks. After `verify.sh`, commit, push, open or update the draft PR, write handoff notes, and return `PUSHED PR #<n>`.
- Handoff notes go under "Notes for next" in the task file: what is done, what is left, and any decision the next builder must not undo.
- Before anything else on an alert or at 100k context: push your work, write the notes, and return `HANDOFF PR #<n>`.
- Keep output short. Pipe anything long through `tail -n 80` or `head -n 80`, send install logs to a file, limit searches (`grep -l`, or `| head`), and read logs only through `scripts/factory/ci-digest.sh <pr>`. Read each screenshot once.

**The COO.**
- Never resume a builder after it returns, or after the watcher reports it over the limit. Start a fresh one with the task file, the PR number and the next step.
- An agent that died on an error below the limit still holds its work, so resume it. Start a fresh one only after 2 failed resumes.
- After `PUSHED`, wait for the watcher's message instead of checking CI.
  - `failure` → a fresh builder gets the output of `scripts/factory/ci-digest.sh <pr>`.
  - `success`, with screenshots or criteria still to check → a fresh builder does `builder.md` §5.4 onward, and returns `PUSHED` after any push.
  - `success`, with nothing left → `gh pr ready <pr>`, then spawn the reviewer.
- If a builder has returned `PUSHED` or `HANDOFF` 3 times without progress on the same CI failure, handle it like `BLOCKED`.
- Check CI yourself only with the one-line `scripts/factory/pr-ci.sh <pr>`, never with JSON dumps.
- After an agent error, wait about 3 minutes before resuming, because these service errors last a few minutes. Never go idle while tasks are in flight.
- Restart your own session between milestones, once `.factory/state.md` is current. Other projects on this account spend the same tokens, so check `omnirush usage` then too.

## What is enforced differently
Claude Code's guard hook (`scripts/factory/guard.sh`) doesn't run here. These checks replace it:
- `scripts/hooks/pre-push` blocks pushes to `main`, and GitHub's branch protection blocks them on the server.
- `scripts/hooks/commit-msg` and the CI `policy` job enforce the owner as author and no AI attribution.

The remaining guard rules are yours to keep, with nothing to stop you. Never run Gradle or an emulator here (use CI), never use `--no-verify`, `--force` or `gh ... --admin`, and never change branch protection or the commit identity.

## Credentials and privacy
Omnirush saves the files an agent reads, runs or creates with its sessions, and says it removes secrets first. So:
- The token here is a dedicated one for this machine, and only for this repo. Only the owner installs or replaces it: `scripts/factory/box-token.sh <file>`, run outside the sandbox.
- Never read, print or copy `.factory/gh_token`, and never put a secret anywhere else in this folder.
- The shared Kaggle account isn't set up here, so the Kaggle overflow is off.

## Only one COO at a time
Two COOs would dispatch the same tasks. `.factory/state.md` is local to each machine, so copy it when moving the factory, and stop the old COO first.
