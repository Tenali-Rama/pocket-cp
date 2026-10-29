# Quality Bar

Builders meet this before marking a PR ready. Reviewers check every point. A PR merges only when there are no blocking findings.

## Definition of done
1. **Acceptance criteria.** Every criterion is met. The reviewer re-checks each one and doesn't trust the PR's evidence table.
2. **Verify.**
   - `scripts/verify.sh` passes locally.
   - PR CI is green: `verify`, `policy` and `ui` (`verify --full` in a fresh checkout is the gate).
3. **Tests.**
   - New behaviour is covered, including at least one edge or failure path.
   - Tests would fail without the change. No tests that only assert mocks.
   - Network and parsing are tested against recorded fixtures in `testdata/`.
4. **Looks right.** Every new or changed screen has Roborazzi screenshots for:
   - phone portrait
   - tablet landscape
   - foldable (unfolded)
   - desktop window
   - each in dark and light
   The builder and the reviewer each **look at** them and confirm:
   - The layout uses the space well. No stretched single column on a tablet, and the spec §5 panes are present.
   - Nothing is clipped, overlapping or cut off. Text is readable.
   - The style matches the design system: dark-first, close to monochrome, rank colours as the only accents, proper spacing.
   - Empty, loading and error states are shown.
5. **Spec.** Matches the referenced spec sections. Nothing is invented beyond them.
6. **Scope.**
   - Changes stay within the task's `touches`, or the PR says why not. No drive-by refactors.
   - An append-only entry (trailing `+`) only adds lines to that file.
7. **Code.**
   - Reads like the surrounding code. Files ≤ 400 lines. No dead code. No TODO without a task id.
   - No `!!`, `lateinit` misuse or `GlobalScope` in non-test code.
   - Errors are handled and shown to the user as clear states, never crashes.
   - Design-system tokens only; no hard-coded colours or dimensions.
8. **Performance** (spec §3).
   - No work on the main thread. Lists are lazy, with stable keys.
   - Cache first; never show a spinner when cached data exists.
   - Recomposition: no unstable params in hot paths.
   - Respect the Codeforces API rate limit (shared request queue).
9. **Accessibility.**
   - Content descriptions on icons.
   - Touch targets ≥ 48dp.
   - Works with large font scale (check the 1.5× screenshot when the task asks for it).
   - Every primary action works from a hardware keyboard.
10. **Security and privacy.**
    - No secrets in code or tests.
    - Sessions and keys go through the encrypted store.
    - The WebView is restricted to Codeforces domains.
    - No analytics, and no data leaves the device except to Codeforces or the user's chosen AI provider.
11. **Docs.**
    - The task file's Notes are filled in.
    - `API.md` is updated if a public interface changed.
    - A lasting decision gets an ADR.
12. **Git.** One task per PR. The owner is the author. No AI attribution.

## Review findings
- **Blocking**: anything that breaks 1–12. Write each one as specific and actionable: `file:line` (or a screenshot name), what to change, and why.
- **Optional**: nits and suggestions. List them separately. They never block a merge.
- Tag every blocking finding with a short slug (e.g. `missing-edge-test`, `scope-creep`, `layout-tablet`, `visual-clipping`). The COO tracks repeat offenders.
