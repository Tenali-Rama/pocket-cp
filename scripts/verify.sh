#!/usr/bin/env bash
# The single verification entry point.
#   verify.sh          light static checks (the VM: factory checks, file size, ktlint)
#   verify.sh --full   + Gradle ciCheck (compile, lint, unit + screenshot tests). CI runs this; never on the VM.
# Prints one line per step; on failure, the tail of that step's log. Full logs: .factory/logs/
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
LOG_DIR=.factory/logs
mkdir -p "$LOG_DIR"
TAIL=${VERIFY_TAIL:-40}
failed=0

step() {
  local name=$1; shift
  local log="$LOG_DIR/$name.log"
  if "$@" >"$log" 2>&1; then
    echo "PASS $name"
  else
    echo "FAIL $name  (full log: $log)"
    tail -n "$TAIL" "$log" | sed 's/^/    /'
    failed=1
  fi
}

step tasks scripts/factory/board.sh --check
step board-test scripts/factory/test-board.sh
step size scripts/factory/check-size.sh

if [ -n "$(git ls-files '*.kt' '*.kts')" ]; then
  if command -v ktlint >/dev/null; then
    step ktlint ktlint --relative '**/*.kt' '**/*.kts' '!**/build/**'
  else
    echo "SKIP ktlint (not installed: run scripts/factory/bootstrap.sh)"
  fi
fi

if [ "${1:-}" = --full ] && [ -x gradlew ]; then
  step gradle ./gradlew ciCheck --stacktrace
fi

if command -v gitleaks >/dev/null; then
  step secrets gitleaks dir --no-banner --redact .
fi

if [ $failed = 0 ]; then echo "VERIFY: PASS"; else echo "VERIFY: FAIL"; fi
exit $failed
