# Regressions fixed by #3712

Compared with the updated #3599:

1. Old-format submissions keep every development and startup period, rather than losing all but one.
2. Explicit end dates stay unchanged. Only new periods without an end date get one calculated.
3. Automatically calculated periods stop at the next submitted assessment instead of extending behind it.
4. Starting the startup phase does not shorten development dates already saved in earlier assessments.
5. A correction is not rejected because of an older assessment that it completely replaces.
6. Previously approved historical assessments stay approved after migration, even when newer questions were never answered. Missing answers remain missing.
7. Old-format, new-format and mixed histories all contribute their periods and used days correctly.
8. Replacing an old assessment also replaces its gaps and remaining validity, so older approvals do not unexpectedly reappear.
9. New phase-based submissions must answer whether the person actively works on the business, even when an end date is supplied. Missing answers are not treated as yes.
10. An incomplete new-format submission is not mistaken for a historical assessment.
11. A positive old-format submission must contain at least one assessed period.
12. Changing an old-format period is recognised as a real change to the assessment.
13. API responses include development and startup periods for both storage formats.
14. API periods use the dates that actually apply after overrides, without overlapping a replacement assessment.
15. A valid migration does not roll back merely because assessments have been split into a different format. Validation does not recalculate saved dates.
16. Migration rolls back if saved periods, outcomes, rule sets, duration or validation status differ from what is expected.
