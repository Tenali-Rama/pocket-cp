#!/usr/bin/env bash
# Perf budget gate (spec §3, ADR 0013): compares one CI perf run with the budgets.
# Usage: perf-budget-check.sh <results-dir> [budgets.json]   (default: config/perf-budgets.json)
#   <results-dir> holds the Macrobenchmark *benchmarkData.json files and apk-bytes.txt (the release APK's size).
# Prints one line per budget, `OK|OVER name value/budget`, or `MISSING name` when the run lacks that metric
# (or it is not a number).
# Exits 1 on any OVER or MISSING.
set -euo pipefail
usage="usage: perf-budget-check.sh <results-dir> [budgets.json]"
dir=${1:?$usage}
budgets=${2:-$(cd "$(dirname "$0")/.." && pwd)/config/perf-budgets.json}
[ -d "$dir" ] || { echo "$usage" >&2; exit 2; }

# Every benchmark of every results file, as one array.
results=$(find "$dir" -name '*benchmarkData.json' -exec cat {} + | jq -s '[.[].benchmarks[]]')

# name|jq filter over $results giving the measured value; the first one counts, none means MISSING.
# The benchmark names are the test methods in benchmark/; memory is the higher of the two startup benchmarks.
metrics=(
  'startup_cold_median_ms|.[] | select(.name == "startupCold") | .metrics.timeToInitialDisplayMs.median'
  'startup_warm_median_ms|.[] | select(.name == "startupWarm") | .metrics.timeToInitialDisplayMs.median'
  'frame_duration_p90_ms|.[] | select(.name == "tabWalk") | .sampledMetrics.frameDurationCpuMs.P90'
  'memory_max_kb|[.[] | select(.name | startswith("startup")) | .metrics.memoryRssAnonMaxKb.median // empty] | max'
)

failed=0

# report <name> <value or empty>
report() {
  local name=$1 value=$2 budget
  budget=$(jq -r --arg n "$name" '.[$n] // empty' "$budgets")
  if [ -z "$budget" ]; then
    echo "no budget for $name in $budgets" >&2
    exit 2
  fi
  if ! [[ $value =~ ^[0-9]+(\.[0-9]+)?([eE][-+]?[0-9]+)?$ ]]; then
    echo "MISSING $name"
    failed=1
    return
  fi
  # One decimal is enough to read; the comparison uses the exact value.
  if jq -en --argjson v "$value" --argjson b "$budget" '$v <= $b' >/dev/null; then
    echo "OK $name $(jq -n --argjson v "$value" '($v * 10 | round) / 10')/$budget"
  else
    echo "OVER $name $(jq -n --argjson v "$value" '($v * 10 | round) / 10')/$budget"
    failed=1
  fi
}

apk=""
if [ -f "$dir/apk-bytes.txt" ]; then apk=$(tr -d '[:space:]' <"$dir/apk-bytes.txt"); fi
report release_apk_bytes "$apk"

for entry in "${metrics[@]}"; do
  name=${entry%%|*}
  report "$name" "$(jq -r "[${entry#*|}] | first" <<<"$results")"
done
exit $failed
