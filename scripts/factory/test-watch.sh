#!/usr/bin/env bash
# Tests the factory watcher against a fake Omnirush database, and the CI digest's log filter.
set -uo pipefail
root=$(git rev-parse --show-toplevel)
if ! command -v python3 >/dev/null; then
  echo "python3 is missing: run scripts/factory/box-setup.sh"
  exit 1
fi
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failed=0
fail() { echo "FAIL $1"; failed=1; }

cat >"$tmp/db.py" <<'PY'
import json, sqlite3, sys, time
db = sqlite3.connect(sys.argv[1])
db.executescript("""create table if not exists session_v2(id, title, parent_id, directory, time_idle, time_updated);
create table if not exists session_message(session_id, type, seq, time_created, data);""")
now = int(time.time() * 1000)
def agent(sid, context, directory="/p", parent="coo", age=0, idle=None, cache_write=0):
    t = now - age
    db.execute("delete from session_v2 where id = ?", (sid,))
    db.execute("insert into session_v2 values (?,?,?,?,?,?)", (sid, "Title " + sid, parent, directory, idle, t))
    seq = db.execute("select count(*) from session_message where session_id = ?", (sid,)).fetchone()[0]
    data = {"tokens": {"input": 1000, "cache": {"read": context - 1000 - cache_write, "write": cache_write}}}
    db.execute("insert into session_message values (?,?,?,?,?)", (sid, "assistant", seq, t, json.dumps(data)))
if sys.argv[2] == "seed":
    agent("low", 90_000)
    agent("high", 120_000)
    agent("cache-write", 110_000, cache_write=20_000)
    agent("other-project", 300_000, directory="/other")
    agent("top-level", 300_000, parent=None)
    agent("old", 300_000, age=3_600_000)
    agent("finished", 300_000, idle=now + 1)
elif sys.argv[2] == "higher":
    agent("high", 155_000)
elif sys.argv[2].startswith(("failed:", "succeeded:")):
    outcome, sid = sys.argv[2].split(":")
    agent(sid, 5_000, parent=None if sid == "coo" else "coo")
    data = {"outcome": outcome, "error": {"message": "upstream_error", "status": 502}}
    db.execute("insert into session_message values (?,?,?,?,?)", (sid, "idle", 1, now + 5000, json.dumps(data)))
db.commit()
PY

db="$tmp/o.db"
python3 "$tmp/db.py" "$db" seed
echo '[{"pr":1,"sha":"aaaaaaaaaa","status":"completed","conclusion":"failure","run":10}]' >"$tmp/ci.json"
run() {
  python3 "$root/scripts/factory/watch.py" --once --dry-run --project /p --db "$db" --state "$tmp/state.json" \
    --ci-file "$tmp/ci.json" --failure-gap "${gap:-0}" 2>&1
}

out=$(run)
grep -q '"Title high"' <<<"$out" || fail "agent over the limit not reported: $out"
grep -q '120k context' <<<"$out" || fail "context size missing: $out"
grep -q '"Title cache-write".*110k context' <<<"$out" || fail "cache writes missing from context: $out"
for quiet in low other-project top-level old finished; do
  grep -q "Title $quiet\"" <<<"$out" && fail "$quiet should not be reported: $out"
done
grep -q 'CI finished' <<<"$out" && fail "first run should only record existing CI results: $out"

out=$(run)
[ -z "$out" ] || fail "second run repeated itself: $out"

python3 "$tmp/db.py" "$db" higher
out=$(run)
grep -q '155k context' <<<"$out" || fail "agent 50k higher not reported again: $out"

python3 "$tmp/db.py" "$db" succeeded:fine
out=$(run)
[ -z "$out" ] || fail "normal stop reported as a failure: $out"

python3 "$tmp/db.py" "$db" failed:broken
out=$(run)
grep -q '"Title broken".*stopped with an error: upstream_error.*resume this same agent' <<<"$out" ||
  fail "failed agent not reported: $out"
out=$(run)
[ -z "$out" ] || fail "failed agent reported twice: $out"

python3 "$tmp/db.py" "$db" failed:broken2
out=$(gap=3600 run)
[ -z "$out" ] || fail "failure reported inside the quiet gap: $out"
out=$(run)
grep -q '"Title broken2"' <<<"$out" || fail "failure lost after the quiet gap: $out"

python3 "$tmp/db.py" "$db" failed:coo
out=$(run)
grep -q 'Your last request stopped with an error' <<<"$out" || fail "COO's own failure not reported: $out"
grep -q 'resume this same agent' <<<"$out" && fail "COO told to resume itself: $out"

cat >"$tmp/ci.json" <<'JSON'
[{"pr":1,"sha":"aaaaaaaaaa","status":"completed","conclusion":"failure","run":10},
 {"pr":2,"sha":"bbbbbbbbbb","status":"in_progress","conclusion":null,"run":20},
 {"pr":3,"sha":"cccccccccc","status":"completed","conclusion":"failure","run":30},
 {"pr":4,"sha":"dddddddddd","status":"completed","conclusion":"success","run":40}]
JSON
out=$(run)
grep -q 'PR #3 at ccccccc: failure, run 30. Give the next builder' <<<"$out" || fail "CI failure not reported: $out"
grep -q 'PR #4 at ddddddd: success, run 40\.' <<<"$out" || fail "CI success not reported: $out"
grep -q 'PR #1' <<<"$out" && fail "already-known CI result repeated: $out"
grep -q 'PR #2' <<<"$out" && fail "running CI reported as finished: $out"
[ -z "$(run)" ] || fail "CI result reported twice"

db_path=$(OPENCODE_DB="$tmp/override.db" XDG_DATA_HOME="$tmp/data" HOME="$tmp/home" python3 - "$root/scripts/factory/watch.py" <<'PY'
import importlib.util
import sys

spec = importlib.util.spec_from_file_location("watch", sys.argv[1])
watch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(watch)
print(watch.default_database())
PY
)
[ "$db_path" = "$tmp/override.db" ] || fail "OPENCODE_DB was not preferred: $db_path"
db_path=$(XDG_DATA_HOME="$tmp/data" HOME="$tmp/home" python3 - "$root/scripts/factory/watch.py" <<'PY'
import importlib.util
import sys

spec = importlib.util.spec_from_file_location("watch", sys.argv[1])
watch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(watch)
print(watch.default_database())
PY
)
[ "$db_path" = "$tmp/data/omnirush/opencode.db" ] || fail "XDG_DATA_HOME was not used: $db_path"
db_path=$(env -u OPENCODE_DB -u XDG_DATA_HOME HOME="$tmp/home" python3 - "$root/scripts/factory/watch.py" <<'PY'
import importlib.util
import sys

spec = importlib.util.spec_from_file_location("watch", sys.argv[1])
watch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(watch)
print(watch.default_database())
PY
)
[ "$db_path" = "$tmp/home/.local/share/omnirush/opencode.db" ] || fail "HOME fallback was not used: $db_path"

cat >"$tmp/gradle.log" <<'LOG'
> Task :core:parser:compileKotlin
e: file:///w/Parser.kt:12:3 Unresolved reference: foo
e: file:///w/Parser.kt:12:3 Unresolved reference: foo
BUILD SUCCESSFUL noise
ParserTest > parsesTitle FAILED
> Task :core:parser:test FAILED
LOG
out=$("$root/scripts/factory/ci-digest.sh" --file "$tmp/gradle.log")
[ "$(grep -c 'Unresolved reference' <<<"$out")" = 1 ] || fail "digest should keep one copy of a repeated error: $out"
grep -q 'parsesTitle FAILED' <<<"$out" || fail "digest lost a failed test: $out"
grep -q 'noise' <<<"$out" && fail "digest kept a line that isn't an error: $out"

exit $failed
