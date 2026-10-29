# Roadmap (v1)

Each milestone ends in an installable APK (release tag `v0.<n>.0`). Spec refs are `spec.sh` headings.
Settings (§15) grow per milestone: each milestone adds the settings section for what it ships.

- M0 — Foundation: Gradle + CI gates, screenshot matrix, design system, adaptive shell, networking core, Maestro smoke, live canary, release APK — spec: 2. Product Principles, 4. Platform & Architecture, 5. Design & Layout, 16. Non-Functional, 18. Known Risks
- M1 — Data base & settings: judge-agnostic domain + `JudgeSource`, Room cache with stale-while-revalidate, DataStore settings, Settings screen (appearance, module on/off framework), About with disclaimer, offline indicator, Macrobenchmark/Baseline Profile budget gate — spec: 3. Performance Budgets, 4. Platform & Architecture, 14. Offline, 15. Settings, 17. Play Store Compliance
- M2 — Browse: Home (contests with countdowns, guest state), contest lists with filters, contest detail problem list, problemset with filters/sort/search/random/spoiler hiding, list prefetch — spec: 7.1 Home, 7.2 Contests, 7.3 Problemset, 3. Performance Budgets
- M3 — Problem view: isolated statement parser, native renderer (LaTeX, images, tables, samples), font/width/EN-RU, bookmarks/notes/personal tags, contest prefetch, list | detail panes, per-page WebView fallback — spec: 7.4 Problem View, 14. Offline, 18. Known Risks
- M4 — Account & browser: WebView login, encrypted session + expiry detection, guest mode, API key, logout, Codeforces-only fallback browser, deep links, share sheet — spec: 6. Authentication, 7.9 Fallback Browser & Deep Links, 16. Non-Functional
- M5 — Code editor: editor core, highlighting, buffers, templates, symbol row, desktop shortcuts, Vim mode, autosave, themes, SAF import/export, statement | editor split with book/tabletop postures — spec: 8. Code Editor, 5. Design & Layout
- M6 — Remote run & submit: test cases panel, Custom Test runs, result diffs, inline compile errors, submit (`Ctrl+Enter`), live verdict + notification, submissions list/detail/diff, offline drafts — spec: 9. Run & Test, 7.5 Submissions, 14. Offline
- M7 — Local compute: device capability check, compute toggle, C++/Python/Java packs via Play Feature Delivery, sandbox, mismatch warning, stress tester — spec: 9. Run & Test, 17. Play Store Compliance
- M8 — Contest participation: register/unregister, contest timer + announcements, standings (filters, pinned user, auto-refresh), virtual participation, Gym & mashups with in-app PDF — spec: 7.2 Contests, 7.6 Gym & Mashups
- M9 — Community: editorials from contest materials, native blog reader + comment tree, recent actions/top blogs, voting/commenting, profiles (rating graph, heatmap, breakdowns), user search, friends, compare — spec: 7.7 Editorials & Blogs, 7.8 Profiles & Friends
- M10 — Practice tools: upsolve tracker, post-contest review, practice recommendations/daily/streak/timer, lists & ladders, stats & analytics, snippet library, notes & scratchpad — spec: 10. Competitive Programmer Tools
- M11 — Calendar & notifications: multi-judge contest calendar, add to Google Calendar, all notification types with per-type toggles (WorkManager/AlarmManager) — spec: 10. Competitive Programmer Tools, 13. Notifications
- M12 — Focus mode & AI: contest mode (DND, screen on, auto-run samples, timer warnings), BYOK AI assistant (providers, hints, explain, translate, contest lock, disclosure) — spec: 11. Focus / Contest Mode, 12. AI Assistant
- M13 — Desktop, input & backup: command palette, shortcut cheat sheet, hover/right-click, drag and drop, stylus; Auto Backup, zip export/import, Drive backup, data settings — spec: 5. Design & Layout, 4. Platform & Architecture, 15. Settings
- M14 — Release readiness: perf budgets met, accessibility pass, opt-in crash reporting, privacy policy + Data Safety, Play listing and large-screen quality — spec: 3. Performance Budgets, 16. Non-Functional, 17. Play Store Compliance, 1. Product
