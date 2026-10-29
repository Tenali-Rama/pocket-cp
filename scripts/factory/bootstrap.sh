#!/usr/bin/env bash
# One-time (idempotent) machine setup for the factory: tools, git identity, hooks. No Android SDK: CI builds.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/factory/owner.env
mkdir -p "$HOME/.local/bin" .factory

command -v jq >/dev/null || sudo apt-get install -y -qq jq
command -v java >/dev/null || sudo apt-get install -y -qq openjdk-17-jre-headless  # ktlint only
command -v gh >/dev/null || sudo apt-get install -y -qq gh
if ! command -v ktlint >/dev/null; then
  curl -fsSL -o "$HOME/.local/bin/ktlint" https://github.com/pinterest/ktlint/releases/download/1.3.1/ktlint
  chmod +x "$HOME/.local/bin/ktlint"
fi
if [ ! -x .factory/venv/bin/kaggle ]; then
  python3 -m venv .factory/venv && .factory/venv/bin/pip install -q kaggle
fi

git config user.name "$OWNER_NAME"
git config user.email "$OWNER_EMAIL"
git config core.hooksPath scripts/hooks
chmod +x scripts/*.sh scripts/factory/*.sh scripts/hooks/*
echo "bootstrap done. Make sure ~/.local/bin is on PATH."
