# Regressions fixed by #3712

Compared with #3599 at `0a3a2ff868`:

1. Validation uses the periods that still apply, so completely replaced historical assessments do not block a correction.
2. API quota calculations include old-format phase periods, not just assessments with a direct phase.
3. New phase-based submissions (V1) are rejected if the required answer about actively working on the business is missing.
4. A positive old-format submission must contain at least one assessed period.
5. Migration validation checks saved assessments without recalculating their end dates.
