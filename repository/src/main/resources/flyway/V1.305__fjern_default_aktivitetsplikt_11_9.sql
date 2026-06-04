-- Fjerner DEFAULT CURRENT_TIMESTAMP fra aktivitetsplikt_11_9-tabeller.
ALTER TABLE aktivitetsplikt_11_9_vurderinger
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE aktivitetsplikt_11_9_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
