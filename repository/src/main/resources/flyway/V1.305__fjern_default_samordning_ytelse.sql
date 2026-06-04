-- Fjerner DEFAULT CURRENT_TIMESTAMP fra samordning_vurderinger og samordning_vurdering.
ALTER TABLE samordning_vurderinger
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE samordning_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
