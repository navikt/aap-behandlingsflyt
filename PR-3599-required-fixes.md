# Regressions fixed by #3712

Compared with the updated #3599:

1. Reassessments preserve inherited assessments exactly as stored, including when no new assessments are submitted.
2. Starting the startup phase does not shorten development dates already saved in earlier assessments.
3. Automatically calculated periods stop at the next submitted assessment instead of extending behind it.
4. Validation uses the periods that still apply, so completely replaced historical assessments do not block a correction.
5. API quota calculations and the stored assessment's timeline include old-format phase periods, not just assessments with a direct phase.
6. New phase-based submissions (V1) are rejected if the required answer about actively working on the business is missing.
7. A positive old-format submission must contain at least one assessed period.
8. Functional equality includes changes to old-format period lists.
9. API periods use the dates that actually apply after overrides, without overlapping a replacement assessment.
10. Migration validation checks saved assessments without recalculating their end dates.
