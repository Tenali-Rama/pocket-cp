#!/usr/bin/env bash
# Fixture tests for shots.sh with a stubbed gh (run by verify.sh).
set -euo pipefail
src=$(cd "$(dirname "$0")" && pwd)/shots.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin" "$tmp/repo/scripts/factory" "$tmp/artifacts"
cp "$src" "$tmp/repo/scripts/factory/shots.sh"
# Stub gh: one PR head, one CI run, artifacts served from $tmp/artifacts/<name>.
cat >"$tmp/bin/gh" <<STUB
#!/usr/bin/env bash
case "\$1 \$2" in
  "pr view") echo 0123456789abcdef ;;
  "run list") echo 42 ;;
  "run download")
    shift 3
    while [ \$# -gt 0 ]; do case \$1 in -n) n=\$2; shift 2 ;; -D) d=\$2; shift 2 ;; *) shift ;; esac; done
    [ -d "$tmp/artifacts/\$n" ] || exit 1
    mkdir -p "\$d" && cp -R "$tmp/artifacts/\$n/." "\$d/" ;;
  *) exit 2 ;;
esac
STUB
chmod +x "$tmp/bin/gh"
export PATH="$tmp/bin:$PATH"
cd "$tmp/repo"
git init -q .
shot=core/x/src/test/screenshots
mkdir -p "$tmp/artifacts/screenshots/$shot" "$shot"
echo new >"$tmp/artifacts/screenshots/$shot/new.png"
echo v2 >"$tmp/artifacts/screenshots/$shot/old.png"
echo v1 >"$shot/old.png"
fail=0

# expect <name> <exit code> <expected output line> <shots.sh args...>
expect() {
  local name=$1 want=$2 line=$3 out got=0
  shift 3
  out=$(scripts/factory/shots.sh "$@" 2>&1) || got=$?
  if [ "$got" != "$want" ]; then
    echo "FAIL $name: exit $got, want $want"
    echo "$out" | sed 's/^/    /'
    fail=1
  elif ! grep -qxF -- "$line" <<<"$out"; then
    echo "FAIL $name: output lacks '$line'"
    echo "$out" | sed 's/^/    /'
    fail=1
  fi
}

# run without ui-* artifacts: lists and summarises, exit 0
expect list-no-ui 0 "run=42 new=1 changed=1 (look at most 8 per round, tablet + phone first)" 9
expect list-no-ui-new 0 "NEW     .factory/shots/9/unit/$shot/new.png" 9
# with a ui-phone artifact: flow shots are listed too
mkdir -p "$tmp/artifacts/ui-phone"
echo flow >"$tmp/artifacts/ui-phone/home.png"
expect list-ui 0 "FLOW    .factory/shots/9/ui-phone/home.png" 9
rm -rf "$tmp/artifacts/ui-phone"
# --accept without ui-* artifacts: copies goldens and reports success
expect accept-no-ui 0 "accepted: new=1 changed=1 — now git add + commit them" 9 --accept
if [ "$(cat "$shot/old.png")" != v2 ] || [ ! -f "$shot/new.png" ]; then
  echo "FAIL accept-no-ui: goldens not copied"
  fail=1
fi
expect after-accept 0 "run=42 new=0 changed=0 (look at most 8 per round, tablet + phone first)" 9

[ $fail = 0 ] && echo "shots.sh: all cases pass"
exit $fail
