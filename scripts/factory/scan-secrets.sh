#!/usr/bin/env bash
# Scan the working tree for secrets, but only files git tracks or would track.
# Git-ignored local files (e.g. .claude/settings.local.json, which holds GH_TOKEN
# on purpose) are skipped; tracked and untracked-but-not-ignored files are scanned
# as they are on disk, with gitleaks' default rules and the repo's own config.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

git ls-files -z --cached --others --exclude-standard | while IFS= read -r -d '' f; do
  # Skip tracked files deleted from the working tree, and gitlink directories.
  if [ -f "$f" ] || [ -L "$f" ]; then
    cp --parents -P -- "$f" "$tmp/"
  fi
done

cd "$tmp"
gitleaks dir --no-banner --redact "$@" .
