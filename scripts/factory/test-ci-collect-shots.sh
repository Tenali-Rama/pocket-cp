#!/usr/bin/env bash
# Fixture tests for scripts/ci-collect-shots.sh (run by verify.sh).
set -euo pipefail
collect=$(cd "$(dirname "$0")/.." && pwd)/ci-collect-shots.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail=0

check() { # check <description> <condition...>
  local what=$1
  shift
  if ! "$@"; then echo "FAIL $what"; fail=1; fi
}

# A repo with one unchanged golden, one changed golden, one new shot (no golden) and a comparison image.
repo=$tmp/repo
g=$repo/core/screenshot/src/test/screenshots
a=$repo/core/screenshot/build/outputs/roborazzi
mkdir -p "$g" "$a" "$repo/app/src/test/screenshots" "$repo/.factory/old/x/src/test/screenshots"
echo same >"$g/same_phone_dark.png"
echo golden >"$g/changed_phone_dark.png"
echo actual >"$a/changed_phone_dark_actual.png"
echo diff >"$a/changed_phone_dark_compare.png"
echo new >"$a/new_tablet_light_actual.png"
echo app >"$repo/app/src/test/screenshots/home_desktop_dark.png"
echo stale >"$repo/.factory/old/x/src/test/screenshots/stale.png"

out=$tmp/out
"$collect" "$out" "$repo" >/dev/null
o=$out/core/screenshot/src/test/screenshots

check "unchanged shot is its golden" grep -qx same "$o/same_phone_dark.png"
check "changed shot is the actual rendering" grep -qx actual "$o/changed_phone_dark.png"
check "new shot is collected without suffix" grep -qx new "$o/new_tablet_light.png"
check "other modules are collected" grep -qx app "$out/app/src/test/screenshots/home_desktop_dark.png"
want=$(printf '%s\n' app/src/test/screenshots/home_desktop_dark.png \
  core/screenshot/src/test/screenshots/changed_phone_dark.png \
  core/screenshot/src/test/screenshots/new_tablet_light.png \
  core/screenshot/src/test/screenshots/same_phone_dark.png)
got=$(cd "$out" && find . -type f | sed 's#^\./##' | sort)
check "exactly one file per shot, no _compare/_actual, nothing from .factory" [ "$got" = "$want" ]
check "refuses a non-empty outdir" bash -c '! "$0" "$1" "$2" 2>/dev/null' "$collect" "$out" "$repo"

exit $fail
