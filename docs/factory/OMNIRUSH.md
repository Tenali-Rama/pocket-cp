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

Wait for CI by polling `scripts/factory/pr-ci.sh <pr>` every few minutes; a long `--wait` may outlive the shell tool's time limit.

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
