-- Fjerner DEFAULT CURRENT_TIMESTAMP fra reduksjon_11_9_grunnlag.
ALTER TABLE reduksjon_11_9_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
