-- Fjerner DEFAULT CURRENT_TIMESTAMP fra formkrav-tabeller.
ALTER TABLE formkrav_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE formkrav_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
