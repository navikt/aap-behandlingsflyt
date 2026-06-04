-- Fjerner DEFAULT CURRENT_TIMESTAMP fra samordning_andre_statlige_ytelser_vurdering.
ALTER TABLE samordning_andre_statlige_ytelser_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
