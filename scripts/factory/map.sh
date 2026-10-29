#!/usr/bin/env bash
# One line per module: its dir and the first sentence of its API.md. Cheap orientation for agents.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
found=0
while IFS= read -r f; do
  found=1
  line=$(grep -vE '^(#|\s*$)' "$f" | head -1)
  printf '%-32s %s\n' "$(dirname "$f")/" "$line"
done < <(git ls-files '*API.md')
[ $found = 1 ] || echo "(no modules with API.md yet)"
