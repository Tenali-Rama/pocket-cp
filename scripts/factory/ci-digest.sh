#!/usr/bin/env bash
# Short summary of why a PR's CI failed, so a fresh builder needn't read whole logs.
#   ci-digest.sh <pr-number>     result, failed jobs, and the error lines of the failed verify run
#   ci-digest.sh --file <log>    only the error lines of a Gradle log
set -uo pipefail
MAX_LINES=60

errors() {
  grep -E '^e: |^> Task .* FAILED|FAILED$|^\* What went wrong|Execution failed for task|\.kt:[0-9]+|AssertionError|ComparisonFailure|Screenshot.*(differ|changed)' "$1" \
    | cut -c1-240 | awk '!seen[$0]++' | head -n "$MAX_LINES"
}

if [ "${1:-}" = --file ]; then
  errors "$2"
  exit 0
fi

root=$(git rev-parse --show-toplevel)
result=$("$root/scripts/factory/pr-ci.sh" "$1")
echo "$result"
run=$(sed -nE 's/.* run=([0-9]+).*/\1/p' <<<"$result")
[ -n "$run" ] || exit 0
case $result in success*) exit 0 ;; esac

gh run view "$run" --json jobs -q '.jobs[] | select(.conclusion == "failure") | "failed job: \(.name)"'
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
if gh run download "$run" -n verify-logs -D "$tmp" >/dev/null 2>&1 && [ -f "$tmp/gradle.log" ]; then
  errors "$tmp/gradle.log"
else
  gh run view "$run" --log-failed | cut -f3- | tail -n "$MAX_LINES" | cut -c1-240
fi
