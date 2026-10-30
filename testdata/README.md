# testdata

Recorded responses that tests serve instead of the network (`core/testing`: `Fixtures`, `FixtureServer`).
Tests never make live calls; every response a test needs lives here.
Debug builds also package `codeforces/api/` as their assets and serve it as the app's data (`app`'s `FixtureJudgeSource`, ADR 0015), so the Maestro flows see these files: renaming or removing one changes what they show.

## Layout
- `codeforces/api/<method>.json`: an API response, e.g. `contest.list.json`.
- `codeforces/api/<method>.<variant>.json`: the same method with other parameters, e.g. `contest.list.gym.json` (`gym=true`).
- `codeforces/api/error.<case>.json`: a `FAILED` envelope, e.g. `error.handle-not-found.json`.

## Recording a fixture
1. Call the public API from your machine, without logging in and without an API key, so no cookie, session or signature is involved:
   `curl -s -o raw.json -w '%{http_code}\n' 'https://codeforces.com/api/contest.list?gym=true'`
   Note the status code: Codeforces sends `FAILED` envelopes with HTTP 400. Map that code in the test.
   Wait at least 2 s between calls (the API's limit).
2. Trim it (e.g. with `jq` or a short Python script): keep the envelope (`status`, `result` or `comment`) and only a few elements, chosen so every field a test asserts appears at least once, and so optional fields are missing at least once.
   Keep the elements unchanged otherwise. Fields our DTOs don't use stay in, because tests rely on them to show that unknown keys are ignored.
3. Pretty-print with 2-space indents and a final newline, so diffs stay readable.
4. Check before committing:
   - no personal data: only public profiles of well-known accounts (as in `user.info.json`), never your own or a friend's handle, e-mail or private fields;
   - no cookies, headers, API keys, `apiSig` values or session ids;
   - the file is small (a few KB); `check-size.sh` skips `testdata/`, so this is on you.

## Current fixtures
| File | Recorded from | HTTP |
|------|---------------|------|
| `codeforces/api/contest.list.json` | `contest.list`, 4 contests (one `BEFORE`; 2274 was added later, its `relativeTimeSeconds` rebased to the first recording so the file keeps one recording moment) | 200 |
| `codeforces/api/contest.list.gym.json` | `contest.list?gym=true`, one fully described Gym contest and one without a start time | 200 |
| `codeforces/api/contest.list.phases.json` | `contest.list` elements with `phase` edited by hand to `CODING`, `PENDING_SYSTEM_TEST`, `SYSTEM_TEST` and an unknown `ARCHIVED`: live phases can't be recorded on demand | 200 |
| `codeforces/api/problemset.problems.json` | `problemset.problems?tags=implementation;math`, 3 problems (one without `points`) and their statistics | 200 |
| `codeforces/api/problemset.problems.mixed.json` | `problemset.problems` (one unrated problem; one problem whose statistics entry was trimmed) plus one problem of `problemset.problems?problemsetName=acmsguru` (no `contestId`) with its statistics | 200 |
| `codeforces/api/user.info.json` | `user.info?handles=tourist;jiangly` | 200 |
| `codeforces/api/user.info.unrated.json` | `user.info?handles=codeforces;tourist` (the official, unrated `codeforces` account) | 200 |
| `codeforces/api/error.handle-not-found.json` | `user.info?handles=no_such_handle_zzzq` | 400 |
| `codeforces/api/error.call-limit.json` | Codeforces' documented "Call limit exceeded" envelope, written by hand: triggering it on purpose would abuse the API | 200 in tests |
