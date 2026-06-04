-- Fjerner DEFAULT CURRENT_TIMESTAMP fra klage_nay- og klage_kontor-tabeller.
ALTER TABLE klage_nay_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE klage_nay_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE klage_kontor_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE klage_kontor_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
