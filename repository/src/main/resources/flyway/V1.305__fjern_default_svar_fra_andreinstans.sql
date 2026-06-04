-- Fjerner DEFAULT CURRENT_TIMESTAMP fra svar_fra_andreinstans-tabeller.
ALTER TABLE svar_fra_andreinstans_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE svar_fra_andreinstans_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
