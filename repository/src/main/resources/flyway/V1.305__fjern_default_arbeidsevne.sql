-- Fjerner DEFAULT CURRENT_TIMESTAMP fra arbeidsevne_grunnlag.
ALTER TABLE arbeidsevne_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
