-- Legg til referanse til tilbakekrevingsbehandling for klage på tilbakekrevingsvedtak.
-- Tilbakekrevingsbehandling har ingen intern BehandlingId (kun ekstern UUID), så den kan
-- ikke lagres i paaklaget_behandling_id (som refererer til BEHANDLING).
ALTER TABLE paaklaget_behandling_vurdering
    ADD COLUMN paaklaget_tilbakekreving_uuid UUID
        REFERENCES tilbakekrevingsbehandling (tilbakekreving_behandling_id) NULL;
