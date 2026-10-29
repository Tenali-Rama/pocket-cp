#!/usr/bin/env bash
# Module conventions (run by verify.sh):
#   - every Gradle module has CLAUDE.md and API.md
#   - no feature/ module depends on another feature
# Modules: each project in an include(...) call of the root settings.gradle.kts (":a:b" -> a/b/),
# plus build-logic/ as one included build. No root settings.gradle.kts -> nothing to check.
# Usage: check-modules.sh [repo-root]   (default: the git top level)
set -euo pipefail
cd "${1:-$(git rev-parse --show-toplevel)}"
[ -f settings.gradle.kts ] || exit 0

# Project paths from include(...) calls; line comments dropped, multi-line calls flattened.
projects() {
  sed -e 's#//.*##' settings.gradle.kts | tr '\n' ' ' |
    grep -oE '(^|[^A-Za-z0-9_.])include[[:space:]]*\([^)]*\)' |
    grep -oE '"[^"]*"' | tr -d '"' | sed -e 's#^:##' -e 's#:#/#g' | sort -u || true
}

lower() { tr -d '-' | tr '[:upper:]' '[:lower:]'; }

bad=0
modules=$(projects)
if [ -d build-logic ]; then modules=$(printf '%s\nbuild-logic\n' "$modules"); fi
while IFS= read -r dir; do
  [ -n "$dir" ] || continue
  for doc in CLAUDE.md API.md; do
    if [ ! -f "$dir/$doc" ]; then
      echo "missing $doc: $dir/"
      bad=1
    fi
  done
done <<<"$modules"

# feature -> feature references, as ":feature:x" or the type-safe accessor "projects.feature.x".
for build in feature/*/build.gradle.kts; do
  [ -f "$build" ] || continue
  self=$(basename "$(dirname "$build")")
  refs=$(sed -e 's#//.*##' "$build" |
    grep -oE ':feature:[A-Za-z0-9_-]+|projects\.feature\.[A-Za-z0-9_]+' | sort -u || true)
  while IFS= read -r ref; do
    [ -n "$ref" ] || continue
    case $ref in
      :feature:*) name=${ref#:feature:} ;;
      *) # accessor "contestList" -> the feature dir "contest-list"
        name=${ref#projects.feature.}
        for d in feature/*/; do
          d=$(basename "$d")
          if [ "$(lower <<<"$d")" = "$(lower <<<"$name")" ]; then name=$d; fi
        done
        ;;
    esac
    [ "$name" = "$self" ] && continue
    echo "feature dependency: feature/$self -> feature/$name"
    bad=1
  done <<<"$refs"
done
exit $bad
