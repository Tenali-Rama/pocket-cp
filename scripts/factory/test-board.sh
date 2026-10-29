#!/usr/bin/env bash
# Fixture tests for board.sh --overlap and the --check trailing-slash rule (run by verify.sh).
set -euo pipefail
src=$(cd "$(dirname "$0")" && pwd)/board.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
cd "$tmp"
git init -q .
mkdir -p docs/tasks app/src feature/a scripts/factory
cp "$src" scripts/factory/board.sh
task() { printf -- '---\nid: %s\nstatus: todo\ndeps: []\ntouches: [%s]\n---\n' "$1" "$2" >"docs/tasks/$1-t.md"; }
fail=0
expect() { # expect <exit code> <task ids...>
  local want=$1 got=0
  shift
  scripts/factory/board.sh --overlap "$@" >/dev/null || got=$?
  if [ "$got" != "$want" ]; then echo "FAIL --overlap $*: exit $got, want $want"; fail=1; fi
}

task 101 app/src/Main.kt
task 102 app/src/
expect 1 101 102 # dir contains file
task 103 app/src
expect 1 101 103 # dir written without trailing /
task 104 feature/a/
task 105 feature/ab/
expect 0 104 105 # sibling with a common prefix
task 106 settings.gradle.kts+
task 107 settings.gradle.kts+
expect 0 106 107 # two append-only entries
task 113 settings.gradle.kts
expect 1 113 106 # append-only vs normal edit
task 108 'gradle/libs.versions.toml, docs/decisions'
task 109 'gradle/libs.versions.toml, docs/decisions/0009-x.md'
expect 0 108 109 # shared files, with and without trailing /
task 110 core/net/API.md
task 111 core/net/API.md
expect 0 110 111 # shared API.md
task 112 feature/x/Main.kt
expect 0 101 112 # unrelated
expect 2 101 999 # unknown id

out=$(scripts/factory/board.sh --check 2>&1 || true)
if [[ $out != *"touches dir 'app/src' needs a trailing /"* ]]; then
  echo "FAIL --check did not flag 'app/src'"
  fail=1
fi
[ $fail = 0 ] && echo "board.sh: all overlap cases pass"
exit $fail
