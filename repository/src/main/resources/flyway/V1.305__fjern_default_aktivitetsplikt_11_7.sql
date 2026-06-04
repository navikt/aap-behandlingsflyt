-- Fjerner DEFAULT CURRENT_TIMESTAMP fra aktivitetsplikt_11_7-tabeller.
ALTER TABLE aktivitetsplikt_11_7_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE aktivitetsplikt_11_7_vurderinger
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE aktivitetsplikt_11_7_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
