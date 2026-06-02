CREATE TABLE krav_vurderinger
(
    id            BIGSERIAL                              NOT NULL PRIMARY KEY,
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE krav_vurdering
(
    id                         BIGSERIAL                              NOT NULL PRIMARY KEY,
    krav_vurderinger_id        BIGINT                                 NOT NULL REFERENCES krav_vurderinger (id),
    journalpost_id             TEXT                                   NOT NULL,
    vurdert_av                 TEXT                                   NOT NULL,
    vurdert_tidspunkt          TIMESTAMP(3)                           NOT NULL,
    krav_type                  TEXT                                   NOT NULL,
    soknadsdato               DATE                                   NULL,
    soknadsdato_aarsak         TEXT                                   NULL,
    mulig_rett_fra             DATE                                   NULL,
    mulig_rett_fra_aarsak      TEXT                                   NULL,
    begrunnelse                TEXT                                   NOT NULL,
    kravdato                   DATE                                   NULL,
    vurdert_i_behandling       BIGINT                                 NOT NULL REFERENCES behandling (id),
    opprettet_tid              TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_krav_vurdering_vurderinger_id ON krav_vurdering (krav_vurderinger_id);
CREATE INDEX idx_krav_vurdering_vurdert_i_behandling ON krav_vurdering (vurdert_i_behandling);

CREATE TABLE krav_grunnlag
(
    id                  BIGSERIAL                              NOT NULL PRIMARY KEY,
    behandling_id       BIGINT                                 NOT NULL REFERENCES behandling (id),
    krav_vurderinger_id BIGINT                                 NULL REFERENCES krav_vurderinger (id),
    aktiv               BOOLEAN      DEFAULT TRUE              NOT NULL,
    opprettet_tid       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_krav_grunnlag_behandling_id ON krav_grunnlag (behandling_id);
CREATE INDEX idx_krav_grunnlag_behandling_aktiv ON krav_grunnlag (behandling_id, aktiv);
