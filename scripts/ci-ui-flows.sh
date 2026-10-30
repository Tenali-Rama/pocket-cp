#!/usr/bin/env bash
# Runs every Maestro flow on the booted CI emulator, one Maestro run per flow, each from a settled device.
#   ci-ui-flows.sh <apk> <flows-dir> <out-dir>
# Writes <out-dir>/<flow>/ (Maestro's output) and <out-dir>/<flow>/report.xml (JUnit). Runs every flow, then
# exits non-zero if any failed. See ADR 0014 for the two failures this prevents:
# - Force-stopping a foreground app makes Android 14 remove its task and arm a ~1 s timer that kills the task's
#   process ("Destroy timeout of remove-task ... Killing <pid>:app.pocketcp: remove task"). A flow whose
#   `launchApp: clearState` starts the app inside that window has its fresh process killed: blank screen, no app.
#   So before each flow the app is stopped here, and the next flow starts only once the system has held no task
#   or activity of the app for STABLE_POLLS polls in a row (spanning more than that timer).
# - A just-booted emulator can raise a system ANR dialog ("Quickstep isn't responding") over the app.
#   Error dialogs are hidden for the whole run; an app crash still fails the flow's assertions.
# Env: ADB, MAESTRO, APP_ID, POLL (seconds between polls), STABLE_POLLS, SETTLE_POLLS (give up after).
set -uo pipefail
usage="usage: ci-ui-flows.sh <apk> <flows-dir> <out-dir>"
apk=${1:?$usage}
flows=${2:?$usage}
out=${3:?$usage}
ADB=${ADB:-adb}
MAESTRO=${MAESTRO:-$HOME/.maestro/bin/maestro}
APP_ID=${APP_ID:-app.pocketcp}
POLL=${POLL:-1}
STABLE_POLLS=${STABLE_POLLS:-3}
SETTLE_POLLS=${SETTLE_POLLS:-60}

# Entries of the activity hierarchy that are still a task (`* Task{... A=<uid>:<app> ...}`) or an activity
# (`Hist #n: ActivityRecord{... <app>/...}`) of the app. Other dump lines may name it after it is gone.
app_records() {
  local app=${APP_ID//./\\.}
  "$ADB" shell dumpsys activity activities |
    grep -cE "\\* Task\\{.* A=[0-9]+:$app[ }]|Hist +#[0-9]+: ActivityRecord\\{[^}]* $app/" || true
}

# Stops the app and waits until no task or activity of it has been left for STABLE_POLLS polls in a row.
settle_app() {
  "$ADB" shell am force-stop "$APP_ID"
  local polls=0 stable=0 seen=0 n=0
  while [ "$stable" -lt "$STABLE_POLLS" ]; do
    if [ "$polls" -ge "$SETTLE_POLLS" ]; then
      echo "::error::$APP_ID still has $n task/activity records after $polls polls" >&2
      return 1
    fi
    n=$(app_records)
    polls=$((polls + 1))
    if [ "$n" -eq 0 ]; then stable=$((stable + 1)); else stable=0 seen=$((seen + 1)); fi
    [ "$stable" -ge "$STABLE_POLLS" ] || sleep "$POLL"
  done
  echo "settled $APP_ID: $polls polls, $seen with leftover records"
}

"$ADB" shell settings put global hide_error_dialogs 1
"$ADB" shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null
"$ADB" install -r "$apk" || exit 1

failed=()
for flow in "$flows"/*.yaml; do
  name=$(basename "$flow" .yaml)
  echo "== $name"
  if ! settle_app; then
    failed+=("$name")
    continue
  fi
  mkdir -p "$out/$name"
  "$MAESTRO" test "$flow" --format junit --output "$out/$name/report.xml" --test-output-dir "$out/$name" ||
    failed+=("$name")
done

[ "${#failed[@]}" -eq 0 ] || { echo "::error::UI flows failed: ${failed[*]}" >&2; exit 1; }
echo "all UI flows passed"
