-- Fjerner DEFAULT CURRENT_TIMESTAMP fra avbryt_revurdering_grunnlag.
ALTER TABLE avbryt_revurdering_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
