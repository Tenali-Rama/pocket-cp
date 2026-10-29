# PocketCP: Unofficial Codeforces Client for Android

## 1. Product
- Unofficial, free, ad-free Android client for Codeforces, built for tablets first and working well on phones.
- Covers all main Codeforces workflows: contests, problemset, Gym, submissions, standings, editorials/blogs and profiles.
- First-class code editor with local and remote running, so users can solve problems end to end in the app.
- Add-on tools for competitive programmers: upsolving, practice, analytics, snippets, stress testing and optional AI.
- No backend. All user data stays on the device.
- Listed on Google Play as **unofficial** and not affiliated with Codeforces.
- Open source on GitHub (GPL-3.0), so users can check how their session and data are handled.
- Launch announced with a post on the Codeforces blog.
- The name is judge-neutral, so later versions can add other judges without renaming.
- Play listing title: *PocketCP: Unofficial Codeforces Client*. Short description: *Solve, run and submit Codeforces problems on phone and tablet.*

## 2. Product Principles
- **Native, not a wrapper.** Every main flow has a native Compose screen. WebView is used only for login, submitting and as a fallback browser, and is never visible in the main flows.
- **Instant.** Cache first and show data immediately, then refresh in the background (stale-while-revalidate). Never show a blank spinner where cached data exists.
- **Focused.** Minimal, dark-first, distraction-free UI. The content (statement, code) gets the most space.
- **Keyboard and stylus friendly.** Every main action works from a hardware keyboard.

## 3. Performance Budgets
- Cold start under 1 s and warm start under 300 ms on a mid-range tablet. Use Baseline Profiles and startup profiling.
- 60/90/120 Hz with no dropped frames when scrolling lists, standings and statements, or when typing in the editor.
- Keystroke-to-render latency in the editor under 16 ms, even with files of 5,000+ lines.
- Problem statement opens in under 100 ms from cache.
- Prefetching:
  - All statements of a contest when it starts or is opened.
  - The next page of lists.
  - The profile data the user is likely to open next.
- Optimistic UI for local actions (bookmark, note, tag).
- Track app size, memory and jank in CI with Macrobenchmark; fail the build if a budget regresses.

## 4. Platform & Architecture
- Kotlin, Jetpack Compose, Material 3 with a custom design system. Min SDK 26.
- MVVM with a unidirectional data flow, Hilt, Coroutines/Flow.
- Judge-agnostic domain layer (Contest, Problem, Submission, User). Codeforces is the first `JudgeSource` implementation, so other judges can be added later without reworking the UI.
- Modular Gradle setup: `core`, `data`, `editor`, `runner`, a module per feature, and dynamic feature modules for the compilers.
- Networking: Retrofit + OkHttp with one cookie jar shared with the WebView. HTTP/2, gzip, ETag/If-Modified-Since.
- Storage:
  - Room: cache, code files, notes, snippets and practice data.
  - DataStore: settings.
  - EncryptedSharedPreferences/Keystore: session and API keys.
- Data sources:
  - Codeforces API for structured data, kept under the 1 request / 2 s rate limit using a request queue, deduplication and backoff.
  - HTML parsing for problem statements, Custom Test, contest materials and Gym. The parser is isolated so it can be fixed quickly when Codeforces changes its markup.
- Backup:
  - Android Auto Backup.
  - Manual export/import (zip of code, snippets, notes and settings).
  - Optional backup to the user's own Google Drive.

## 5. Design & Layout
- **Visual style:**
  - Minimal, dark-first, close to monochrome. Codeforces rank colours are the only strong accents.
  - Light theme and an AMOLED black theme also available.
  - Typography: a clean sans-serif for the UI, and a monospaced font for code with ligatures that can be toggled (JetBrains Mono by default; user-selectable).
  - Subtle motion that never blocks: shared-element transitions, no decorative animation.
- **Adaptive layout (window size classes):**
  - Phone: bottom navigation, single pane.
  - Tablet: navigation rail, list–detail panes, and a resizable split between statement and editor.
  - Tablet split layouts: statement | editor, editor | output, problem | editorial.
- Works in portrait and landscape and in multi-window/split-screen, and keeps state across rotation, resizing and process death.
- Officially supported and tested in v1: phones, tablets, foldables, ChromeOS and Samsung DeX.
- **Foldables:**
  - Uses the Jetpack WindowManager fold-posture APIs.
  - Book posture: statement on one half, editor on the other.
  - Tabletop posture: editor on the bottom half, output or statement on the top.
  - Moves smoothly between folded and unfolded.
- **ChromeOS / DeX (desktop mode):**
  - Freely resizable windows, with the layout adapting at every size.
  - Keyboard and mouse are the primary input: right-click menus, scroll wheel, drag and drop of source files.
  - No phone-only patterns there (e.g. no gestures that rely on swipe-only navigation).
- Input:
  - Full hardware keyboard support, with a shortcut cheat sheet (`Ctrl+/`).
  - Mouse/trackpad hover and right-click menus.
  - Stylus: scribble in the scratchpad, and select text in statements.
- Command palette (`Ctrl+K`) to jump to a problem, contest or user, or to run any action.

## 6. Authentication
- Log in through a secure WebView. The password is never stored.
- Session cookie kept in encrypted storage. Detect when the session expires and ask the user to log in again without losing their work.
- Guest mode: everything read-only works without logging in.
- Optional Codeforces API key/secret for authenticated API methods.
- Logging out clears the session (local code and notes are kept unless the user chooses to wipe them).

## 7. Core Codeforces Features

### 7.1 Home
- Upcoming and running contests with countdowns.
- User's rating and rank.
- Recent verdicts.
- "Continue" card for the last problem worked on.
- Upsolve queue preview and daily practice suggestion.

### 7.2 Contests
- Lists of upcoming, running and past contests; filter by division and type.
- Register and unregister.
- Contest detail: problems with solve status, timer, announcements.
- Virtual participation with its own timer and standings.
- Standings:
  - Official/unofficial, friends only, filter by country.
  - Current user pinned.
  - Paginated, with auto-refresh while the contest is live.

### 7.3 Problemset
- Filters: tags, rating range, solved/unsolved, contest/division.
- Sort and search by name or ID (`1900A`).
- Status badge on each problem; random problem matching the filters.
- Option to hide tags and ratings, so users don't get spoilers while practising.

### 7.4 Problem View
- Statement rendered natively:
  - LaTeX, so math is never broken.
  - Images and tables.
  - Samples with copy and send-to-editor buttons.
- Adjustable font size, reading width and English/Russian toggle.
- Bookmark the problem; private notes; personal tags.
- Links to the editorial and contest materials.

### 7.5 Submissions
- My submissions, globally and per contest, filtered by verdict and language.
- Submission detail: code, verdict, time and memory, failing test where visible.
- Diff between my submissions.
- View other users' accepted code where Codeforces allows it.
- Live verdict tracking (queue → test #N → final verdict) and a notification when it finishes.

### 7.6 Gym & Mashups
- Browse and search Gym with filters.
- Full contest features inside Gym; PDF statements open in-app.
- Join mashups (creating them is out of scope).

### 7.7 Editorials & Blogs
- Editorial resolved from contest materials and linked from each problem.
- Native blog reader: code blocks, LaTeX, spoilers, images, collapsible comment tree.
- Recent actions and top blogs.
- Commenting and voting through the session; anything not built natively opens in the fallback browser.

### 7.8 Profiles & Friends
- Profile:
  - Rank-coloured handle, rating, max rating, contribution.
  - Rating graph and activity heatmap.
  - Solved breakdown by tag and by rating.
- Search users; friends list; compare two users side by side.

### 7.9 Fallback Browser & Deep Links
- In-app browser that keeps the logged-in session, for any page without a native screen.
- `codeforces.com` links open the matching native screen. Share sheet on all entities.

## 8. Code Editor
- Native Compose editor (built in-house or on Sora-Editor). Must hit the latency budget in §3.
- Basics:
  - Syntax highlighting for C++, Python, Java, Kotlin and others.
  - Line numbers, current-line highlight, auto-indent, bracket matching and auto-close.
  - Code folding, multiple cursors, find/replace with regex.
  - Undo/redo with persistent history.
- Autocomplete:
  - Keywords, STL/standard library, identifiers in the file and snippets.
  - Fast, ranked, and never blocks typing.
- Formatting: format document with clang-format for C++ (included in the compiler pack) and basic formatting for other languages.
- Tabs and files:
  - Several buffers for each problem: `main`, `brute`, `gen`, `scratch`.
  - Quick switching between them, and splitting the editor.
- Templates: a default template per language, inserted into new files automatically.
- Soft keyboard:
  - A row of symbol keys above the keyboard (`{ } [ ] ( ) < > ; : & | Tab`), customisable.
  - Swipe on the row to move the cursor.
  - Never covered by the IME.
- Hardware keyboard:
  - Familiar desktop shortcuts (VS Code style).
  - Optional **Vim mode** with normal, insert and visual modes and the common motions.
- Autosave:
  - Every edit is saved locally per problem.
  - No unsaved state is lost on crash or when Android kills the app.
- Code lives in a project for each problem, so users can come back to it later from the problem screen.
- Themes: editor colour schemes that match the app (dark, light, AMOLED); font and size settings.
- Import and export files with the Storage Access Framework.

## 9. Run & Test
- **Test cases panel** for each problem:
  - Samples are added automatically from the statement.
  - Users can add, edit and delete their own tests.
  - Expected output stored with each test.
- **Run modes:**
  - **Remote:** Codeforces Custom Test via the session. Matches the judge's compiler; needs the internet.
  - **Local (on-device):** offline and fast. Optional download through Play Feature Delivery.
  - The user picks a default mode and can switch per run.
- **Local compute is optional:**
  - On first launch, check the device's RAM and CPU. Below the threshold (e.g. under 4 GB RAM), recommend remote-only mode and don't prompt the user to download a compiler pack.
  - Users can turn local compute on or off at any time in Settings. Turning it off switches every run to remote and offers to uninstall the compiler packs to free space.
  - Every feature that needs local compute (stress tester, offline runs) is hidden or shows a remote alternative when local compute is off.
- **Local compiler packs** (v1), each a separate optional download:
  - C++: clang, C++20, with common flags, `bits/stdc++.h` and clang-format.
  - Python 3: CPython.
  - Java: compiled with ECJ, then D8 to dex, then run on ART.
- Packaging: ship the binaries as native libraries in dynamic feature modules so they comply with Play's exec and W^X rules. Never download them from outside Play.
- Sandbox for local runs: CPU time limit, memory limit and output size cap, plus a kill switch. User code gets no network access.
- Results:
  - Each test shows pass/fail, a side-by-side output diff, stderr, time and memory.
  - Option to ignore whitespace and use float tolerance when comparing.
  - Compile errors are shown inline in the editor at the right lines.
- **Mismatch warning:**
  - When local and remote runs of the same code and input give different results, show a clear warning with both outputs side by side.
  - The warning names likely causes: undefined behaviour, uninitialised variables, a compiler or version difference, or an integer size difference.
  - The warning also appears before submitting if the latest local and remote runs disagree.
- **Stress tester:**
  - Runs `gen` → `main` and `brute` in a loop and stops at the first mismatch.
  - Shows the failing input, saving it as a test takes one tap.
  - Settings for the number of iterations and the time limit.
- **Submit** from the editor with one action (`Ctrl+Enter`). Optional warning when the samples haven't passed.

## 10. Competitive Programmer Tools
- **Upsolve tracker:**
  - Unsolved problems from contests the user took part in are added automatically.
  - Mark items done or skipped; sort by rating.
- **Practice:**
  - Recommends problems near the user's rating (+0 to +300), weighted towards weak tags.
  - Daily problem and a streak.
  - Practice timer per problem, which records time-to-solve.
- **Problem lists / ladders:**
  - Users build their own lists.
  - Rating ladders (e.g. 800 → 2400) generated from the problemset.
  - Progress shown for each list.
- **Stats & analytics:**
  - Solved by tag and by rating; weak tags (high wrong-answer rate, low solve count).
  - Contest performance history: rank, delta, problems solved per contest.
  - Verdict breakdown; average time-to-solve.
  - Activity heatmap and streaks; compare with friends.
- **Snippet library:**
  - Personal library of code snippets (DSU, segment tree, mod int, etc.), with tags and search.
  - Insert from the editor via autocomplete prefix or a shortcut.
  - A starter pack of common snippets is included.
- **Contest calendar:**
  - Codeforces, AtCoder, CodeChef and LeetCode contests shown in one list (calendar and reminders only).
  - Add to Google Calendar with one tap; filter by judge and division.
- **Notes & scratchpad:** notes per problem in Markdown, plus a stylus/finger drawing scratchpad for working on paper-style ideas.
- **Post-contest review:**
  - Summary for each contest: what was solved and when, wrong-answer penalties, rating change.
  - Unsolved problems are pushed to the upsolve queue.

## 11. Focus / Contest Mode
- Turns on automatically when a contest the user is registered for starts (can be disabled), or manually.
- Hides everything outside the contest; navigation shrinks to the contest's problems, editor and standings.
- Auto Do Not Disturb for the length of the contest (asks for notification-policy permission).
- Keeps the screen on.
- Runs the samples automatically on save and shows pass/fail inline.
- The contest timer is always visible; low-time warnings at 15 and 5 minutes.
- AI features are hard-locked (see §12).

## 12. AI Assistant (Optional, Bring Your Own Key)
- Off by default. The user enables it and enters their own API key.
- Provider and model picker (e.g. Anthropic, OpenAI, Google, any OpenAI-compatible endpoint); the user also sets the endpoint URL and model name.
- Keys are stored encrypted on the device and sent only to the provider the user configured.
- Features:
  - Progressive hints for a problem (nudge → approach → full idea).
  - Explain my code / find the bug against a failing test.
  - Explain the editorial; translate a statement.
- **Contest lock:**
  - Every AI feature is disabled and hidden while any rated contest the user is registered for, or taking part in, is running. This includes virtual contests the user is currently in.
  - No override exists, because Codeforces rules forbid AI help during contests.
- Clear disclosure: which data (statement, code) gets sent to the provider and when.

## 13. Notifications
- Contest reminders with configurable lead times; reminder when registration opens; reminders for other judges' contests from the calendar.
- Final verdicts, rating changes, and daily practice/streak reminders.
- Each type can be turned on or off. Scheduled locally with WorkManager and AlarmManager (no push server).

## 14. Offline
- Viewed statements, editorials, bookmarks, lists and all user data are available offline.
- Local run and the stress tester work fully offline.
- Clear offline indicator; submissions made offline are saved as drafts and are never sent automatically.

## 15. Settings
- Features:
  - Turn any optional module on or off: local compute, AI, stress tester, practice and streaks, stats, the other-judges calendar, focus mode auto-start.
  - A disabled module is removed from the UI completely.
- Appearance:
  - Theme (dark/light/AMOLED).
  - App and editor fonts and sizes; ligatures.
  - Density (compact/comfortable).
- Editor:
  - Keymap (default/Vim), tab size, auto-close, autocomplete on/off.
  - Symbol row layout.
  - Templates per language.
- Run: default mode (local or remote), compiler packs (install/remove), time and memory limits, compiler flags.
- Focus mode behaviours; notification preferences.
- AI: provider, model, endpoint, key, enable/disable.
- Data: cache size, export/import, Drive backup, wipe.
- Account: login/logout, Codeforces API key, mirror domain.

## 16. Non-Functional
- Reliability:
  - Graceful handling when Codeforces is down or rate-limited, or when the HTML parser fails (fall back to the WebView for that page).
  - Crash-free sessions ≥ 99.8%.
- Accessibility:
  - TalkBack labels and dynamic text scaling.
  - WCAG AA contrast.
  - Touch targets of at least 48 dp.
- Security:
  - HTTPS only; secrets encrypted.
  - The WebView is restricted to Codeforces domains.
  - No logging of credentials or code.
- Privacy: no analytics by default. Crash reporting is opt-in and contains no personal data or code.
- Localization: English in v1, with all strings externalised.

## 17. Play Store Compliance
- Listing:
  - Clearly unofficial, with no Codeforces logo or branding as the app icon.
  - Disclaimer in the description and in the app's About screen.
- Privacy policy; Data Safety form: data stored locally, AI requests go only to the provider the user configures, nothing sold or shared.
- Target the current required API level; meet Google Play's large-screen app quality guidelines; tablet and phone screenshots.
- Compiler packs delivered only through Play Feature Delivery.

## 18. Known Risks
- **HTML parsing breakage:**
  - Statements, Custom Test, submitting, contest materials and Gym depend on Codeforces' page markup. A redesign or Cloudflare bot checks can break them without warning.
  - v1 mitigation: the parser is isolated, falls back to the WebView per page, and is fixed through regular Play updates.
  - Revisit in a future version: downloadable parser rules (within Play's rules on downloading code).

## 19. Out of Scope (v1)
- Backend, user accounts, phone↔tablet sync (later versions; v1 has manual export/import and Drive backup only).
- Live rating change prediction (v2).
- Creating contests, problems or mashups; Polygon.
- Local compilers beyond C++, Python and Java (Kotlin, Rust and others in later versions).
- Full support for other judges (calendar only in v1).
- Admin and moderation tools.
