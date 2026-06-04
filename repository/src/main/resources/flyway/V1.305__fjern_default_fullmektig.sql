-- Fjerner DEFAULT CURRENT_TIMESTAMP fra fullmektig-tabeller.
-- Tidspunktet settes eksplisitt i kode.
ALTER TABLE fullmektig_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE fullmektig_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

