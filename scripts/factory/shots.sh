#!/usr/bin/env bash
# Screenshots from a PR's latest CI run, compared with the goldens in your working tree.
#   shots.sh <pr>            download, then list NEW / CHANGED screenshots (open them with Read) and emulator flow shots
#   shots.sh <pr> --accept   copy NEW / CHANGED images over the goldens (only after you looked at them), then commit
# Contract (set up in M0): artifact "screenshots" mirrors repo paths of the goldens
# (<module>/src/test/screenshots/<name>.png); artifacts "ui-<device>" hold Maestro screenshots.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
pr=$1
sha=$(gh pr view "$pr" --json headRefOid -q .headRefOid)
run=$(gh run list --commit "$sha" --workflow ci --limit 1 --json databaseId -q '.[0].databaseId // empty')
[ -n "$run" ] || { echo "no CI run for PR #$pr head ${sha:0:8} yet"; exit 3; }
dir=.factory/shots/$pr
rm -rf "$dir" && mkdir -p "$dir"
gh run download "$run" -n screenshots -D "$dir/unit" >/dev/null 2>&1 || echo "no 'screenshots' artifact in run $run"
for d in phone tablet; do gh run download "$run" -n "ui-$d" -D "$dir/ui-$d" >/dev/null 2>&1 || true; done

new=0; changed=0
while IFS= read -r f; do
  rel=${f#"$dir/unit/"}
  if [ ! -f "$rel" ]; then
    new=$((new + 1)); [ "${2:-}" = --accept ] && { mkdir -p "$(dirname "$rel")"; cp "$f" "$rel"; } || echo "NEW     $f"
  elif ! cmp -s "$f" "$rel"; then
    changed=$((changed + 1)); [ "${2:-}" = --accept ] && cp "$f" "$rel" || echo "CHANGED $f   (golden: $rel)"
  fi
done < <(find "$dir/unit" -name '*.png' 2>/dev/null | sort)
find "$dir"/ui-* -name '*.png' 2>/dev/null | sort | sed 's/^/FLOW    /'
if [ "${2:-}" = --accept ]; then echo "accepted: new=$new changed=$changed — now git add + commit them"
else echo "run=$run new=$new changed=$changed (look at most 8 per round, tablet + phone first)"; fi
