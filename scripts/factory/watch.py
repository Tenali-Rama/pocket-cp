#!/usr/bin/env python3
"""Tells the COO about events that would otherwise cost tokens to notice.

Run it on the Omnirush machine, outside the sandbox, in its own tmux session (see docs/factory/OMNIRUSH.md).
It messages the COO's tmux session when:
  - an agent's context passes the limit, and again every --step tokens above it
  - an agent stops on an error
  - CI finishes for an open PR's head commit

  watch.py [--once] [--dry-run]    --dry-run prints messages instead of sending them
"""
import argparse
import json
import os
import re
import sqlite3
import subprocess
import sys
import time
import urllib.request

ACTIVE_MS = 20 * 60 * 1000


def agent_rows(db, project):
    return db.execute(
        "select id, title, parent_id, time_idle, time_updated from session_v2 where directory = ?", (project,)
    ).fetchall()


def context_alerts(db, project, limit, step, seen, now_ms):
    """One line per running agent that has crossed a new threshold."""
    lines = []
    for sid, title, parent, idle, updated in agent_rows(db, project):
        if not parent or now_ms - updated > ACTIVE_MS:
            continue
        row = db.execute(
            "select data, time_created from session_message where session_id = ? and type = 'assistant' "
            "order by seq desc limit 1",
            (sid,),
        ).fetchone()
        if not row or (idle and idle >= row[1]):
            continue
        tokens = json.loads(row[0]).get("tokens") or {}
        context = tokens.get("input", 0) + (tokens.get("cache") or {}).get("read", 0)
        # Below the limit this is negative, so it never beats the -1 default.
        band = (context - limit) // step
        if band > seen.get(sid, -1):
            seen[sid] = band
            lines.append(
                f'Agent "{title}" ({sid}) is at {context // 1000}k context, limit {limit // 1000}k. Tell it to push '
                "its work, write handoff notes in the task file and return. Then start a fresh agent from those notes."
            )
    return lines


def failure_alerts(db, project, started_ms, seen):
    """One line per agent whose latest message is a failed stop that we haven't reported."""
    lines = []
    for sid, title, parent, _idle, _updated in agent_rows(db, project):
        row = db.execute(
            "select type, seq, time_created, data from session_message where session_id = ? order by seq desc limit 1",
            (sid,),
        ).fetchone()
        if not row or row[0] != "idle" or row[2] < started_ms or seen.get(sid) == row[1]:
            continue
        data = json.loads(row[3])
        if data.get("outcome") != "failed":
            continue
        seen[sid] = row[1]
        message = str((data.get("error") or {}).get("message"))[:120]
        if parent:
            lines.append(
                f'Agent "{title}" ({sid}) stopped with an error: {message}. Don\'t go idle. Wait about 3 minutes, then '
                "resume this same agent, because its context and worktree still hold its work. Start a fresh one only "
                "if it is over the context limit or 2 resumes have failed."
            )
        else:
            lines.append(f"Your last request stopped with an error: {message}. Continue where you left off.")
    return lines


def github(path, token):
    request = urllib.request.Request(
        "https://api.github.com/" + path,
        headers={"Authorization": "Bearer " + token, "Accept": "application/vnd.github+json"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def github_ci_runs(project):
    """[{pr, sha, status, conclusion, run}] for the head commit of every open PR."""
    with open(os.path.join(project, ".factory/gh_token")) as handle:
        token = handle.read().strip()
    url = subprocess.check_output(["git", "-C", project, "remote", "get-url", "origin"], text=True)
    repo = re.sub(r"\.git$", "", re.sub(r"^.*github\.com[:/]", "", url.strip()))
    runs = []
    for pull in github(f"repos/{repo}/pulls?state=open&per_page=30", token):
        sha = pull["head"]["sha"]
        found = github(f"repos/{repo}/actions/runs?head_sha={sha}&per_page=20", token)["workflow_runs"]
        for run in found:
            if run["name"] == "ci":
                runs.append(
                    dict(pr=pull["number"], sha=sha, status=run["status"], conclusion=run["conclusion"], run=run["id"])
                )
                break
    return runs


def ci_alerts(runs, seen, notify):
    lines = []
    for run in runs:
        key = f"{run['pr']}:{run['sha']}"
        if run["status"] != "completed" or key in seen:
            continue
        seen[key] = run["conclusion"]
        if notify:
            line = f"CI finished for PR #{run['pr']} at {run['sha'][:7]}: {run['conclusion']}, run {run['run']}."
            if run["conclusion"] != "success":
                line += f" Give the next builder the output of scripts/factory/ci-digest.sh {run['pr']}."
            lines.append(line)
    return lines


def send(session, text):
    """Pastes text into the COO's chat and submits it. Returns False if that session isn't running."""
    if subprocess.run(["tmux", "has-session", "-t", session], capture_output=True).returncode != 0:
        return False
    subprocess.run(["tmux", "load-buffer", "-b", "factory-watch", "-"], input=text, text=True, check=True)
    subprocess.run(["tmux", "paste-buffer", "-d", "-p", "-b", "factory-watch", "-t", session], check=True)
    time.sleep(2)
    subprocess.run(["tmux", "send-keys", "-t", session, "Enter"], check=True)
    time.sleep(5)
    # The app holds new messages while the COO waits on an agent in the foreground, until that wait is backgrounded.
    screen = subprocess.run(["tmux", "capture-pane", "-p", "-t", session], capture_output=True, text=True).stdout
    if "Press ctrl+b" in screen:
        subprocess.run(["tmux", "send-keys", "-t", session, "C-b"], check=True)
    return True


def load_state(path, now):
    if os.path.exists(path):
        with open(path) as handle:
            return json.load(handle), False
    return {"started": now, "context": {}, "failures": {}, "ci": {}, "last_failure_note": 0}, True


def tick(args, state, first_run):
    now = time.time()
    db = sqlite3.connect(f"file:{args.db}?mode=ro", uri=True)
    try:
        lines = context_alerts(db, args.project, args.limit, args.step, state["context"], int(now * 1000))
        # A provider outage also fails the COO's own replies, so report failures at most this often.
        if now - state["last_failure_note"] >= args.failure_gap:
            failures = failure_alerts(db, args.project, int(state["started"] * 1000), state["failures"])
            if failures:
                state["last_failure_note"] = now
                lines += failures
    finally:
        db.close()
    try:
        if args.ci_file:
            with open(args.ci_file) as handle:
                runs = json.load(handle)
        else:
            runs = github_ci_runs(args.project)
        lines += ci_alerts(runs, state["ci"], notify=not first_run)
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        # Never print the exception's request details; the type is enough to see that GitHub was unreachable.
        print(f"watch: CI check skipped ({type(error).__name__})", file=sys.stderr)
    return lines


def main():
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", default=root)
    parser.add_argument("--db", default=os.path.expanduser("~/.local/share/omnirush/opencode.db"))
    parser.add_argument("--state", default=os.path.join(root, ".factory/watch-state.json"))
    parser.add_argument("--session", default="coo")
    parser.add_argument("--limit", type=int, default=100_000)
    parser.add_argument("--step", type=int, default=50_000)
    parser.add_argument("--interval", type=int, default=120)
    parser.add_argument("--failure-gap", type=int, default=600)
    parser.add_argument("--ci-file", help="tests only: read CI runs from this JSON file")
    parser.add_argument("--once", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    while True:
        state, first_run = load_state(args.state, time.time())
        previous = json.dumps(state)
        lines = tick(args, state, first_run)
        delivered = True
        if lines:
            text = "Factory watcher: " + " ".join(lines)
            if args.dry_run:
                print(text)
            else:
                delivered = send(args.session, text)
        # Nothing is marked as seen unless it reached the COO, so a stopped COO hears about it after a restart.
        saved = state if delivered else json.loads(previous)
        with open(args.state, "w") as handle:
            json.dump(saved, handle)
        if args.once:
            return
        time.sleep(args.interval)


if __name__ == "__main__":
    main()
