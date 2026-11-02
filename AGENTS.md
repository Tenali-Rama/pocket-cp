# PocketCP (Omnirush entry point)

Omnirush reads this file, not `CLAUDE.md`. The project rules are the same on both.

Before anything else:
1. Read `CLAUDE.md`. All of it applies here.
2. Read `docs/factory/OMNIRUSH.md`. Where it differs from a Claude-specific instruction, it wins.

Roles:
- Told you are the **COO** → read `docs/factory/COO.md` and follow it, using `docs/factory/OMNIRUSH.md` for how to run agents.
- Told you are a **planner / builder / reviewer** → read `.claude/agents/<role>.md` and follow it.
- Any other direct work → follow `.claude/agents/builder.md`.

Never print, read aloud or copy `.factory/gh_token`.
