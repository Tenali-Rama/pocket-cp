#!/usr/bin/env bash
# Claude Code PreToolUse hook for Bash: blocks commands that break factory rules.
# Exit 2 = block; the message goes back to the agent.
input=$(cat)
cmd=$(jq -r '.tool_input.command // ""' <<<"$input")
cwd=$(jq -r '.cwd // "."' <<<"$input")
deny() { echo "BLOCKED by factory guard: $1" >&2; exit 2; }
has() { grep -qiE -e "$1" <<<"$cmd"; }

push=$(grep -oE 'git( -C [^ ]+)? push[^;&|]*' <<<"$cmd" || true)
if [ -n "$push" ]; then
  in_push() { grep -qE "$1" <<<"$push"; }
  in_push '[ :+](main|master)( |$)' && deny "never push to main — push your task branch and open a PR."
  in_push ' (-f|--force)( |$)' && deny "plain force-push is not allowed — use --force-with-lease on your own branch."
  branch=$(git -C "$cwd" rev-parse --abbrev-ref HEAD 2>/dev/null)
  if [ "$branch" = main ] && ! in_push ' [^ -][^ ]*:[^ ]+' && ! in_push ' v[0-9]+\.[0-9]+\.[0-9]+'; then
    deny "you are on main — switch to a task branch before pushing."
  fi
fi
# This VM (2 CPU, 3 GB, shared) cannot build Android: CI does it.
if has '(^|[;&|(][[:space:]]*)([^[:space:]]*/)?gradlew?([[:space:]]|$)' && ! has 'gradlew? +(wrapper|--version|help)( |$)'; then
  deny "no Gradle builds on this VM — push and use scripts/factory/pr-ci.sh (or kaggle-verify.sh when Actions is queued)."
fi
has '(^|[;&|(][[:space:]]*)([^[:space:]]*/)?(emulator|avdmanager)([[:space:]]|$)' && deny "no emulator on this VM — CI runs the ui job."
has '(pkill|killall)( |$)' && deny "no broad kills — kill only a PID you started."
has '--no-verify' && deny "--no-verify is not allowed; fix the hook failure instead."
has 'git (-c [^ ]+ )*commit.*--author' && deny "commits are authored by the owner only (repo git config)."
has 'git config.*user\.(name|email)' && ! has 'scripts/factory/bootstrap' && deny "do not change the commit identity."
has 'co-authored-by:.*(claude|anthropic)|generated with \[?claude' && deny "no AI attribution in commits or PRs."
has 'git( -C [^ ]+)? (update-ref|branch -f) +(refs/heads/)?main' && deny "main changes only through merged PRs."
has 'gh .*--admin|branches/[^ ]*/protection|gh auth (logout|refresh|token)' && deny "admin, branch-protection and token actions are owner-only."
has 'kaggle kernels (delete|cancel)' && deny "never delete or cancel Kaggle kernels (the account is shared with ntl-factory)."
exit 0
