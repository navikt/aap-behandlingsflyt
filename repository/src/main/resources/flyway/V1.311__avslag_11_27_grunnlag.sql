CREATE TABLE avslag_11_27_vurderinger
(
    id            BIGSERIAL    NOT NULL PRIMARY KEY,
    opprettet_tid TIMESTAMP(3) NOT NULL
);

CREATE TABLE avslag_11_27_vurdering
(
    id                                  BIGSERIAL    NOT NULL PRIMARY KEY,
    journalpost_id                      TEXT         NOT NULL,
    avslag_11_27_vurderinger_id         BIGINT       NOT NULL REFERENCES avslag_11_27_vurderinger (id),
    har_annen_full_ytelse               BOOLEAN      NOT NULL,
    har_sykepenger_grunnlag_over_2G     BOOLEAN      NULL,
    skal_avslas_11_27                   BOOLEAN      NOT NULL,
    begrunnelse                         TEXT         NOT NULL,
    vurdert_i_behandling                BIGINT       NOT NULL REFERENCES behandling (id),
    opprettet_tid                       TIMESTAMP(3) NOT NULL,
    vurdert_av                          TEXT         NOT NULL
);

CREATE TABLE avslag_11_27_grunnlag
(
    id                              BIGSERIAL               NOT NULL PRIMARY KEY,
    behandling_id                   BIGINT                  NOT NULL REFERENCES behandling (id),
    avslag_11_27_vurderinger_id     BIGINT                  NOT NULL REFERENCES avslag_11_27_vurderinger (id),
    aktiv                           BOOLEAN DEFAULT TRUE    NOT NULL,
    opprettet_tid                   TIMESTAMP(3)            NOT NULL
);

CREATE INDEX idx_avslag_11_27_vurdering_vurderinger_id ON avslag_11_27_vurdering (avslag_11_27_vurderinger_id);

CREATE UNIQUE INDEX idx_avslag_11_27_grunnlag_behandling_aktiv
    on avslag_11_27_grunnlag (behandling_id)
    where (aktiv = true);
