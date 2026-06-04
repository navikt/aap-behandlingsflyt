-- Fjerner DEFAULT CURRENT_TIMESTAMP fra student-tabeller.
ALTER TABLE student_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE student_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
