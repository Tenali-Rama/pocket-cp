# 0014 — UI flows run one at a time from a settled device; main CI runs are never cancelled

- **Status**: accepted
- **Context**: main went red on commits that never touched the app. Three causes: (1) a flow starting while the
  previous flow's app was still foreground — the force-stop arms Android 14's ~1 s remove-task timer, which killed
  the new process ("Destroy timeout of remove-task … Killing … remove task"); (2) a boot-time "Quickstep isn't
  responding" dialog covering the app; (3) `cancel-in-progress` cancelling main's run when the next merge landed.
- **Decision**: CI runs flows via `scripts/ci-ui-flows.sh`: error dialogs hidden, one Maestro run per flow, and
  before each the app is stopped and the next flow waits until the activity dump shows no task or activity of the
  app for 3 polls a second apart. On main, every commit gets its own concurrency group and is never cancelled.
- **Consequences**: flows stay self-contained (`stopApp` + `launchApp: clearState`) and must not depend on the
  order or on state from another flow. Do not go back to `maestro test .maestro/` or add blind retries.
