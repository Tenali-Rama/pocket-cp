#!/usr/bin/env bash
# Owner: install a new GitHub token without showing it. Reads it from a file, stores it privately, deletes the file.
#   rotate-token.sh <file-with-token>
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
src=$1
install -d -m 700 "$HOME/.config/pocketcp"
tr -d ' \r\n' <"$src" >"$HOME/.config/pocketcp/gh_token" && chmod 600 "$HOME/.config/pocketcp/gh_token"
rm -f "$src"
python3 - <<'PY'
import json, os, pathlib
p = pathlib.Path('.claude/settings.local.json')
d = json.loads(p.read_text()) if p.exists() else {}
d.setdefault('env', {})['GH_TOKEN'] = open(os.path.expanduser('~/.config/pocketcp/gh_token')).read().strip()
p.write_text(json.dumps(d, indent=2) + '\n'); os.chmod(p, 0o600)
PY
GH_TOKEN=$(cat "$HOME/.config/pocketcp/gh_token") gh api -i "repos/$(sed -n 's/^GH_REPO=//p' scripts/factory/owner.env)" 2>/dev/null \
  | grep -i '^github-authentication-token-expiration' || echo "token ok (no expiry)"
echo "token installed. Restart the COO session so agents pick it up."
