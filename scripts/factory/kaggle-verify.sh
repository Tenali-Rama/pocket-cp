#!/usr/bin/env bash
# CI overflow: run `scripts/verify.sh --full` for a pushed branch on a Kaggle CPU kernel (4 cores, ~30 GB).
# Advisory only — GitHub Actions stays the merge gate. Use when Actions runs sit queued > 15 min.
#   kaggle-verify.sh <branch>   prints PASS|FAIL and where the logs/screenshots landed (.factory/kaggle/<branch>/)
# One kernel at a time (lock). The Kaggle account is shared with ntl-factory: never touch its kernels.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/factory/owner.env
branch=$1
[ -n "$KAGGLE_USER" ] || { echo "kaggle overflow not configured (KAGGLE_USER in owner.env)"; exit 2; }
git ls-remote --exit-code origin "refs/heads/$branch" >/dev/null || { echo "push $branch first"; exit 2; }
mkdir -p .factory
exec 9>.factory/kaggle.lock
flock -w 3600 9 || { echo "kaggle busy for 60 min, giving up"; exit 3; }

slug=pocketcp-verify
work=.factory/kaggle/job && out=.factory/kaggle/$(tr '/' '-' <<<"$branch")
rm -rf "$work" "$out" && mkdir -p "$work" "$out"
cat >"$work/kernel-metadata.json" <<JSON
{"id": "$KAGGLE_USER/$slug", "title": "$slug", "code_file": "run.py", "language": "python",
 "kernel_type": "script", "is_private": true, "enable_gpu": false, "enable_internet": true,
 "dataset_sources": [], "competition_sources": [], "kernel_sources": []}
JSON
cat >"$work/run.py" <<PY
import subprocess, pathlib
sh = r'''
set -x
export DEBIAN_FRONTEND=noninteractive ANDROID_HOME=/tmp/sdk
apt-get update -qq && apt-get install -y -qq openjdk-17-jdk-headless unzip >/dev/null
export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which javac))))
mkdir -p \$ANDROID_HOME/cmdline-tools && cd /tmp
curl -fsSLo t.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q t.zip -d \$ANDROID_HOME/cmdline-tools && mv \$ANDROID_HOME/cmdline-tools/cmdline-tools \$ANDROID_HOME/cmdline-tools/latest
yes | \$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
git clone -q --depth 1 -b "$branch" https://github.com/$GH_REPO /tmp/repo && cd /tmp/repo
scripts/verify.sh --full; rc=\$?
mkdir -p /kaggle/working/out && cp -r .factory/logs /kaggle/working/out/ 2>/dev/null
find . -path '*/build/outputs/roborazzi/*' -name '*.png' -exec cp --parents {} /kaggle/working/out/ \; 2>/dev/null
[ \$rc = 0 ] && echo PASS > /kaggle/working/out/result.txt || echo FAIL > /kaggle/working/out/result.txt
'''
subprocess.run(["bash", "-c", sh])
PY
"$KAGGLE_BIN" kernels push -p "$work" >/dev/null
for _ in $(seq 1 120); do
  sleep 30
  st=$("$KAGGLE_BIN" kernels status "$KAGGLE_USER/$slug" 2>/dev/null || true)
  case $st in *complete*|*error*|*cancel*) break ;; esac
done
"$KAGGLE_BIN" kernels output "$KAGGLE_USER/$slug" -p "$out" >/dev/null 2>&1 || true
res=$(cat "$out/out/result.txt" 2>/dev/null || echo "FAIL (kernel: ${st:-timeout})")
echo "$res  logs: $out/out/logs/  screenshots: $out/out/"
[ "${res%% *}" = PASS ]
