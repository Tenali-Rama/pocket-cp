#!/usr/bin/env bash
# Fixture tests for scripts/ci-ui-flows.sh (run by verify.sh), with fake adb and maestro.
set -euo pipefail
run=$(cd "$(dirname "$0")/.." && pwd)/ci-ui-flows.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail=0

check() { # check <description> <condition...>
  local what=$1
  shift
  if ! "$@"; then echo "FAIL $what"; fail=1; fi
}
fails() { ! "$@"; }

# Fake adb: logs every call. `dumpsys activity activities` shows the app's task and activity for the first
# $LEFTOVER dumps after each force-stop (the platform still removing it), then only unrelated lines.
cat >"$tmp/adb" <<'EOF'
#!/usr/bin/env bash
echo "adb $*" >>"$CALLS"
case "$*" in
  "shell am force-stop app.pocketcp") echo 0 >"$STATE/dumps" ;;
  "shell dumpsys activity activities")
    n=$(($(cat "$STATE/dumps") + 1)); echo "$n" >"$STATE/dumps"
    echo "  * Task{1a2b #3 type=home A=10100:com.android.launcher3 U=0 visible=true}"
    echo "  mLastPausedActivity: ActivityRecord{9f u0 app.pocketcp/.MainActivity t8 f}"
    echo "  * Task{4c5d #4 type=standard A=10142:app.pocketcpx U=0}"
    if [ "$n" -le "$LEFTOVER" ]; then
      echo "  * Task{c7b4 #8 type=standard A=10141:app.pocketcp U=0 visible=false}"
      echo "    * Hist  #0: ActivityRecord{f792 u0 app.pocketcp/.MainActivity t8 f} isExiting}"
    fi ;;
esac
EOF
# Fake maestro: logs the flow; fails the flows listed in $FAIL_FLOWS.
cat >"$tmp/maestro" <<'EOF'
#!/usr/bin/env bash
echo "maestro $2" >>"$CALLS"
case " $FAIL_FLOWS " in *" $(basename "$2" .yaml) "*) exit 1 ;; esac
EOF
chmod +x "$tmp/adb" "$tmp/maestro"
mkdir -p "$tmp/flows" "$tmp/state"
touch "$tmp/flows/a.yaml" "$tmp/flows/b.yaml" "$tmp/app.apk"

go() { # go <leftover dumps> <failing flows> [settle polls]: runs the script, returns its exit code
  : >"$tmp/calls"
  rm -rf "$tmp/out"
  CALLS=$tmp/calls STATE=$tmp/state LEFTOVER=$1 FAIL_FLOWS=$2 ADB=$tmp/adb MAESTRO=$tmp/maestro \
    POLL=0 STABLE_POLLS=3 SETTLE_POLLS=${3:-60} "$run" "$tmp/app.apk" "$tmp/flows" "$tmp/out" >"$tmp/log" 2>&1
}
# Prints the calls with each run of dumpsys collapsed to "dumpsys xN".
calls() {
  awk '/dumpsys/ { n++; next } n { print "dumpsys x" n; n = 0 } { print } END { if (n) print "dumpsys x" n }' \
    "$tmp/calls"
}

check "passes when every flow passes" go 2 ""
want="adb shell settings put global hide_error_dialogs 1
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS
adb install -r $tmp/app.apk
adb shell am force-stop app.pocketcp
dumpsys x5
maestro $tmp/flows/a.yaml
adb shell am force-stop app.pocketcp
dumpsys x5
maestro $tmp/flows/b.yaml"
check "hides error dialogs, then per flow: stop, wait for 2 leftover + 3 clean dumps, run that flow alone" \
  [ "$(calls)" = "$want" ]
check "reports what the wait saw" grep -q "settled app.pocketcp: 5 polls, 2 with leftover records" "$tmp/log"
check "writes each flow's output to its own dir" [ -d "$tmp/out/a" -a -d "$tmp/out/b" ]

check "no leftover: waits for 3 clean dumps only" go 0 ""
check "no leftover: dumpsys x3 per flow" [ "$(calls | grep -c '^dumpsys x3$')" = 2 ]

check "fails when one flow fails" fails go 0 "a"
check "still runs the flows after a failed one" grep -qx "maestro $tmp/flows/b.yaml" "$tmp/calls"
check "names the failed flow" grep -q "UI flows failed: a$" "$tmp/log"

check "fails when the app never settles" fails go 99 "" 10
check "does not run a flow on an unsettled device" fails grep -q "^maestro" "$tmp/calls"
check "says why it gave up" grep -q "still has 2 task/activity records after 10 polls" "$tmp/log"

exit $fail
