#!/usr/bin/env bash
# Fixture tests for perf-budget-check.sh (run by verify.sh), against testdata/perf/ and its own budgets.json,
# so changing config/perf-budgets.json never breaks them.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
check=$root/scripts/perf-budget-check.sh
data=$root/testdata/perf
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail=0

# expect <name> <results dir> <exit code> <expected output, one line per budget>
expect() {
  local name=$1 dir=$2 want=$3 lines=$4 out got=0
  out=$("$check" "$dir" "$data/budgets.json" 2>&1) || got=$?
  if [ "$got" != "$want" ] || [ "$out" != "$lines" ]; then
    echo "FAIL $name: exit $got, want $want"
    diff <(echo "$lines") <(echo "$out") | sed 's/^/    /' || true
    fail=1
  fi
}

expect pass "$data/pass" 0 "OK release_apk_bytes 3521024/4000000
OK startup_cold_median_ms 745.7/1000
OK startup_warm_median_ms 171/250
OK frame_duration_p90_ms 11.8/16
OK memory_max_kb 55296/60000"

# Bigger APK and slower cold start than budgeted; the rest stays within.
expect over "$data/over" 1 "OVER release_apk_bytes 4194304/4000000
OVER startup_cold_median_ms 1180.4/1000
OK startup_warm_median_ms 171/250
OK frame_duration_p90_ms 11.8/16
OK memory_max_kb 55296/60000"

# A run whose benchmarks crashed has an APK size but no results: every benchmark metric fails.
cp "$data/pass/apk-bytes.txt" "$tmp/"
expect missing "$tmp" 1 "OK release_apk_bytes 3521024/4000000
MISSING startup_cold_median_ms
MISSING startup_warm_median_ms
MISSING frame_duration_p90_ms
MISSING memory_max_kb"

exit $fail
