#!/usr/bin/env bash
# Owner, outside the Omnirush sandbox: install or replace this machine's GitHub token without showing it.
#   box-token.sh <file-with-token>    Reads the file, stores the token privately, deletes the file.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
src=$1
mkdir -p .factory
(umask 077; tr -d ' \r\n' <"$src" >.factory/gh_token)
rm -f "$src"
echo "token installed in .factory/gh_token (git-ignored). Next Omnirush session: scripts/factory/preflight.sh"
