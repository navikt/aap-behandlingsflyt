-- Fjerner DEFAULT CURRENT_TIMESTAMP fra paaklaget_behandling-tabeller.
ALTER TABLE paaklaget_behandling_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE paaklaget_behandling_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
