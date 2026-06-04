-- Fjerner DEFAULT CURRENT_TIMESTAMP fra samordning_ufore_vurdering.
ALTER TABLE samordning_ufore_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
