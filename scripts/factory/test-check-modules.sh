#!/usr/bin/env bash
# Fixture tests for check-modules.sh (run by verify.sh).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
check=$here/check-modules.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail=0

# fixture <name> <module dirs...>: a tree whose modules all have CLAUDE.md and API.md
fixture() {
  local root=$tmp/$1
  shift
  mkdir -p "$root"
  for m in "$@"; do
    mkdir -p "$root/$m"
    touch "$root/$m/CLAUDE.md" "$root/$m/API.md"
  done
  echo "$root"
}

# expect <name> <root> <exit code> [expected output line]
expect() {
  local name=$1 root=$2 want=$3 line=${4:-} out got=0
  out=$("$check" "$root" 2>&1) || got=$?
  if [ "$got" != "$want" ]; then
    echo "FAIL $name: exit $got, want $want"
    echo "$out" | sed 's/^/    /'
    fail=1
  elif [ -n "$line" ] && ! grep -qxF "$line" <<<"$out"; then
    echo "FAIL $name: output lacks '$line'"
    echo "$out" | sed 's/^/    /'
    fail=1
  fi
}

# pass: nested project paths, includeBuild is not a module, comments ignored
r=$(fixture pass app core/model feature/contests)
cat >"$r/settings.gradle.kts" <<'EOF'
pluginManagement { includeBuild("build-logic-elsewhere") }
include(":app")
include(":core:model")
include(":feature:contests")
// include(":feature:commented-out")
EOF
expect pass "$r" 0

# missing docs
r=$(fixture missing app)
mkdir -p "$r/core/net"
touch "$r/core/net/CLAUDE.md"
printf 'include(":app")\ninclude(":core:net")\n' >"$r/settings.gradle.kts"
expect missing-api "$r" 1 "missing API.md: core/net/"
mkdir -p "$r/core/ui"
printf 'include(":app")\ninclude(":core:ui")\n' >"$r/settings.gradle.kts"
expect missing-both "$r" 1 "missing CLAUDE.md: core/ui/"

# feature -> feature, both notations; a self-reference is fine
r=$(fixture feat feature/a feature/b feature/contest-list core/model)
printf 'include(":feature:a", ":feature:b", ":feature:contest-list", ":core:model")\n' >"$r/settings.gradle.kts"
printf 'dependencies {\n  implementation(project(":core:model"))\n}\n' >"$r/feature/a/build.gradle.kts"
expect feature-clean "$r" 0
printf 'dependencies {\n  implementation(project(":feature:b"))\n}\n' >"$r/feature/a/build.gradle.kts"
expect feature-colon "$r" 1 "feature dependency: feature/a -> feature/b"
printf 'dependencies {\n  implementation(projects.feature.contestList)\n}\n' >"$r/feature/b/build.gradle.kts"
expect feature-accessor "$r" 1 "feature dependency: feature/b -> feature/contest-list"
rm "$r/feature/a/build.gradle.kts" "$r/feature/b/build.gradle.kts"
printf 'android { namespace = "x" }\nval self = project(":feature:contest-list")\n' >"$r/feature/contest-list/build.gradle.kts"
expect feature-self "$r" 0

# no settings.gradle.kts -> empty module set -> pass, even with undocumented dirs
r=$(fixture none)
mkdir -p "$r/app" "$r/build-logic"
expect no-settings "$r" 0

# build-logic: one module, docs at build-logic/ only, convention/ is not separate
r=$(fixture buildlogic app build-logic)
mkdir -p "$r/build-logic/convention"
touch "$r/build-logic/convention/build.gradle.kts"
printf 'include(":app")\n' >"$r/settings.gradle.kts"
expect build-logic "$r" 0
rm "$r/build-logic/API.md"
expect build-logic-missing "$r" 1 "missing API.md: build-logic/"

# multi-argument include, spread over lines
r=$(fixture multi app core/model)
printf 'include(\n    ":app",\n    ":core:model",\n    ":core:data",\n)\n' >"$r/settings.gradle.kts"
expect multi-include "$r" 1 "missing CLAUDE.md: core/data/"

# verify.sh runs both steps
for s in "step modules scripts/factory/check-modules.sh" "step modules-test scripts/factory/test-check-modules.sh"; do
  if ! grep -qF "$s" "$here/../verify.sh"; then
    echo "FAIL verify.sh lacks '$s'"
    fail=1
  fi
done

[ $fail = 0 ] && echo "check-modules.sh: all cases pass"
exit $fail
