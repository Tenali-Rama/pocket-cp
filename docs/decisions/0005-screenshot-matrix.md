# 0005 — Screenshot matrix, golden naming and CI gate

- **Status**: accepted
- **Context**: QUALITY.md §4 needs every screen rendered on phone, tablet, foldable and desktop in dark and light, compared against committed goldens, and visible to agents through `shots.sh`.
- **Decision**:
  - Devices (dp, rendered at density 1 so pixels = dp): phone 411×914 (portrait), tablet 1280×800 (landscape), foldable unfolded 673×841, desktop window 1440×900.
  - Goldens: `<module>/src/test/screenshots/<name>_<device>_<theme>.png`, `device` ∈ phone|tablet|foldable|desktop, `theme` ∈ dark|light. Extra shots (AMOLED, 1.5× font) put the variant in `<name>`.
  - Roborazzi + Robolectric native graphics on SDK 34 via `pocketcp.android.compose`; `ciCheck` runs `verifyRoborazziDebug`, which fails on a diff and on a missing golden.
  - CI collects `<name>_actual.png` from verify (or the golden when unchanged) into the `screenshots` artifact; goldens are only ever CI output accepted with `shots.sh --accept`.
- **Consequences**: Changing a size or the naming re-renders every golden, so do it in its own task. Goldens rendered elsewhere (a laptop, another JDK) won't match CI pixels: never commit them.
