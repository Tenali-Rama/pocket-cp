#!/usr/bin/env bash
# Tests the git hooks: pre-push blocks main, and commit-msg strips AI attribution from any tool.
set -uo pipefail
root=$(git rev-parse --show-toplevel)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failed=0
fail() { echo "FAIL $1"; failed=1; }

push() { printf 'refs/heads/x aaa %s bbb\n' "$1" | "$root/scripts/hooks/pre-push" origin url >/dev/null 2>&1; }
push refs/heads/main && fail "pre-push allowed main"
push refs/heads/master && fail "pre-push allowed master"
push refs/heads/task/001-x || fail "pre-push blocked a task branch"
push refs/tags/v0.3.0 || fail "pre-push blocked a release tag"

git init -q "$tmp/repo"
mkdir -p "$tmp/repo/scripts/factory"
cp "$root/scripts/factory/owner.env" "$tmp/repo/scripts/factory/"
source "$root/scripts/factory/owner.env"
cat >"$tmp/msg" <<'MSG'
042: a real subject
Co-authored-by: Claude <noreply@anthropic.com>
Co-Authored-By: Omnirush <agent@omnirush.ai>
Co-authored-by: gpt-6-astra <noreply@openai.com>
Co-authored-by: A Person <person@example.com>
Generated with Omnirush
MSG
(cd "$tmp/repo" && GIT_AUTHOR_NAME="$OWNER_NAME" GIT_AUTHOR_EMAIL="$OWNER_EMAIL" "$root/scripts/hooks/commit-msg" "$tmp/msg") \
  || fail "commit-msg rejected the owner"
grep -qiE 'claude|omnirush|gpt' "$tmp/msg" && fail "commit-msg kept AI attribution"
grep -q '042: a real subject' "$tmp/msg" || fail "commit-msg dropped the subject"
grep -q 'A Person' "$tmp/msg" || fail "commit-msg dropped a human co-author"
(cd "$tmp/repo" && GIT_AUTHOR_NAME=Someone GIT_AUTHOR_EMAIL=someone@example.com "$root/scripts/hooks/commit-msg" "$tmp/msg") 2>/dev/null \
  && fail "commit-msg accepted another author"

[ "$failed" = 0 ] && echo "hooks: all passed"
exit "$failed"
