#!/usr/bin/env bash
# COO start check: is the factory able to run? One line per check; exits non-zero if any fail.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/factory/owner.env
export GH_REPO
fail=0
check() {
  local name=$1 fix=$2; shift 2
  if "$@" >/dev/null 2>&1; then echo "OK   $name"; else echo "FAIL $name — fix: $fix"; fail=1; fi
}
warn() {
  local name=$1 note=$2; shift 2
  if "$@" >/dev/null 2>&1; then echo "OK   $name"; else echo "WARN $name — $note"; fi
}

check "owner.env filled"    "owner: set GH_REPO / OWNER_NAME / OWNER_EMAIL in scripts/factory/owner.env" \
  bash -c "[ '$GH_REPO' != OWNER/REPO ] && [ '$OWNER_EMAIL' != owner@example.com ]"
check "gh installed"        "run scripts/factory/bootstrap.sh"          command -v gh
check "gh authenticated"    "owner: gh auth login --with-token (fine-grained PAT)" gh auth status
check "gh repo access"      "token needs access to $GH_REPO"            gh repo view "$GH_REPO"
check "main protected"      "owner: ! scripts/factory/protect-main.sh"  gh api "repos/$GH_REPO/branches/main/protection"
check "jq installed"        "run scripts/factory/bootstrap.sh"          command -v jq
check "ktlint installed"    "run scripts/factory/bootstrap.sh"          command -v ktlint
check "git push access"     "token/key with write access to $GH_REPO"   git ls-remote origin
check "commit identity"     "run scripts/factory/bootstrap.sh"          test "$(git config user.email)" = "$OWNER_EMAIL"
check "commit hook active"  "run scripts/factory/bootstrap.sh"          test "$(git config core.hooksPath)" = scripts/hooks
check "on main, clean tree" "commit/stash local changes, git switch main" \
  bash -c '[ "$(git rev-parse --abbrev-ref HEAD)" = main ] && [ -z "$(git status --porcelain)" ]'
check "main up to date"     "git pull --ff-only"                        git pull --ff-only -q
check "board valid"         "fix task frontmatter (board.sh --check)"   scripts/factory/board.sh --check
warn  "kaggle overflow"     "optional: CI overflow disabled (set KAGGLE_USER, run bootstrap)" \
  bash -c "[ -n '$KAGGLE_USER' ] && '$KAGGLE_BIN' kernels list --mine --page-size 1"
exp=$(gh api -i "repos/$GH_REPO" 2>/dev/null | sed -n 's/^github-authentication-token-expiration: //Ip' | tr -d '\r')
if [ -n "$exp" ] && [ "$(date -d "$exp" +%s)" -lt "$(date -d '+3 days' +%s)" ]; then
  echo "WARN token expires $exp — owner: put a new token in a file and run: ! scripts/factory/rotate-token.sh <file>"
fi
mkdir -p .factory
exit $fail
