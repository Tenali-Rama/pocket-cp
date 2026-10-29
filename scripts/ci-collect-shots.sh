#!/usr/bin/env bash
# Collects the current rendering of every Roborazzi screenshot for scripts/factory/shots.sh.
#   ci-collect-shots.sh <outdir> [repo-root]
# Writes <outdir>/<module>/src/test/screenshots/<name>.png, exactly one file per shot. CI runs it after
# `verify.sh --full`, also when that failed: an unchanged shot is its golden; a new or changed one is the
# `<name>_actual.png` Roborazzi verify left in <module>/build/outputs/roborazzi/, renamed to <name>.png.
set -euo pipefail
out=$(mkdir -p "${1:?usage: ci-collect-shots.sh <outdir> [repo-root]}" && cd "$1" && pwd)
[ -z "$(ls -A "$out")" ] || { echo "outdir $out is not empty" >&2; exit 2; }
cd "${2:-$(git rev-parse --show-toplevel)}"
GOLDEN=src/test/screenshots
ACTUAL=build/outputs/roborazzi

pngs() {
  find . \( -path ./.git -o -path ./.gradle -o -path ./.factory -o -path ./.claude \) -prune \
    -o -type f -path "$1" -print | sed 's#^\./##' | sort
}
mapfile -t goldens < <(pngs "*/$GOLDEN/*.png")
mapfile -t actuals < <(pngs "*/$ACTUAL/*_actual.png")

for f in "${goldens[@]}"; do
  mkdir -p "$out/$(dirname "$f")"
  cp "$f" "$out/$f"
done
for f in "${actuals[@]}"; do
  dest=$out/${f%%/"$ACTUAL"/*}/$GOLDEN
  mkdir -p "$dest"
  cp "$f" "$dest/$(basename "$f" _actual.png).png"
done
echo "collected $(find "$out" -name '*.png' | wc -l) screenshots (${#actuals[@]} new or changed) into $out"
