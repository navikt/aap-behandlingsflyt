# Regressions fixed by #3712

Compared with #3599 at `0a3a2ff868`:

All existing assessments must be migrated by `BackfillEtableringVurderinger` before #3599 is merged.
The effective-period validation change and its legacy-only reproducer (issue 1, #3743) have therefore been removed.
Existing issue IDs are retained so the remaining learning PRs keep their references.

| Issue | Learning PR | Branch | Fix |
|---|---|---|---|
| 2 | #3744 | `learn/pr3599-10-api-legacy-kvoter` | API quota calculations include old-format phase periods, not just assessments with a direct phase. |
| 3 | #3738 | `learn/pr3599-6-aktivt-arbeid` | New phase-based submissions (V1) are rejected if the required answer about actively working on the business is missing. |
| 4 | #3735 | `learn/pr3599-7-legacy-uten-perioder` | A positive old-format submission must contain at least one assessed period. |
| 5 | #3745 | `learn/pr3599-11-migrering-validering` | Migration validation checks saved assessments without recalculating their end dates. |

Learning PRs contain one intentionally failing regression each, without the corresponding production fix.
The shared end-date assertion correction is a test-only prerequisite, already included here.
