-- Additiv migrering: Ved tilbakerulling av applikasjonen kan tabellen beholdes ubrukt.
CREATE TABLE LOVVALG_DELVURDERINGER (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE LOVVALG_DELVURDERING (
    id BIGSERIAL PRIMARY KEY,
    vurderinger_id BIGINT NOT NULL REFERENCES LOVVALG_DELVURDERINGER(id),
    vurdert_i_behandling BIGINT NOT NULL REFERENCES BEHANDLING(id),
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versjon INTEGER NOT NULL CHECK (versjon > 0),
    vurdering JSONB NOT NULL,
    relevant_dato DATE NOT NULL
);

CREATE INDEX idx_lovvalg_delvurdering_sett ON LOVVALG_DELVURDERING (vurderinger_id);

CREATE TABLE LOVVALG_DELVURDERING_GRUNNLAG (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES BEHANDLING(id),
    vurderinger_id BIGINT NOT NULL REFERENCES LOVVALG_DELVURDERINGER(id),
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lovvalg_delvurdering_behandling
    ON LOVVALG_DELVURDERING_GRUNNLAG (behandling_id);

CREATE INDEX idx_lovvalg_delvurdering_grunnlag_sett
    ON LOVVALG_DELVURDERING_GRUNNLAG (vurderinger_id);

CREATE UNIQUE INDEX uidx_lovvalg_delvurdering_aktiv
    ON LOVVALG_DELVURDERING_GRUNNLAG (behandling_id) WHERE aktiv;
