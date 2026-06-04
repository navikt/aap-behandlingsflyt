-- Fjerner DEFAULT CURRENT_TIMESTAMP fra avbryt_aktivitetspliktbehandling-tabeller.
ALTER TABLE avbryt_aktivitetspliktbehandling_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE avbryt_aktivitetspliktbehandling_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
