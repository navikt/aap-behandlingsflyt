-- Fjerner DEFAULT CURRENT_TIMESTAMP fra bistand-tabeller.
ALTER TABLE bistand_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE bistand_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
