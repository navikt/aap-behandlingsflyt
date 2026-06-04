-- Fjerner DEFAULT CURRENT_TIMESTAMP fra meldeplikt_fritak_grunnlag.
ALTER TABLE meldeplikt_fritak_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
