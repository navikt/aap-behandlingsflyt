-- Fjerner DEFAULT CURRENT_TIMESTAMP fra trekk_klage-tabeller.
ALTER TABLE trekk_klage_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE trekk_klage_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
