-- Fjerner DEFAULT CURRENT_TIMESTAMP fra behandlende_enhet_grunnlag.
-- Tidspunktet settes eksplisitt i kode.
ALTER TABLE behandlende_enhet_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

