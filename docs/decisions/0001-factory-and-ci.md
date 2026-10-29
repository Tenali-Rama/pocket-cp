# 0001 — Factory runs on a small VM; CI builds everything

- **Status**: accepted
- **Context**: The factory VM has 2 CPU / 3 GB RAM, is shared, and can't run Gradle or an emulator. The owner has no bigger machine.
- **Decision**:
  - GitHub Actions (public repo: free, 4 vCPU / 16 GB / KVM) runs compile, tests, Roborazzi screenshots and Maestro emulator flows. It is the only merge gate.
  - The VM runs static checks only; the guard hook blocks Gradle and the emulator.
  - A Kaggle CPU kernel (`kaggle-verify.sh`) is advisory overflow when Actions is queued.
- **Consequences**:
  - Builders iterate through draft PRs, about 8–15 min per CI round.
  - Agents verify visuals by downloading the CI screenshots (`shots.sh`) and looking at them.
  - Goldens are only ever produced by CI.
