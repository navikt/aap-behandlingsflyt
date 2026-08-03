CREATE TABLE stonadsperiode_vurderinger
(
    id            BIGSERIAL    NOT NULL PRIMARY KEY,
    opprettet_tid TIMESTAMP(3) NOT NULL
);

CREATE TABLE stonadsperiode_vurdering
(
    id                             BIGSERIAL    NOT NULL PRIMARY KEY,
    referanse                      TEXT         NOT NULL,
    stonadsperiode_vurderinger_id  BIGINT       NOT NULL REFERENCES stonadsperiode_vurderinger (id),
    har_hatt_ordinar_siste_52_uker BOOLEAN      NOT NULL,
    har_gjenvaerende_kvote         BOOLEAN      NOT NULL,
    relevant_krav_type             TEXT         NOT NULL,
    begrunnelse                    TEXT         NOT NULL,
    vurdert_i_behandling           BIGINT       NOT NULL REFERENCES behandling (id),
    vurdert_tidspunkt              TIMESTAMP(3) NOT NULL,
    vurdert_av                     TEXT         NOT NULL
);

CREATE TABLE stonadsperiode_grunnlag
(
    id                            BIGSERIAL            NOT NULL PRIMARY KEY,
    behandling_id                 BIGINT               NOT NULL REFERENCES behandling (id),
    stonadsperiode_vurderinger_id BIGINT               NOT NULL REFERENCES stonadsperiode_vurderinger (id),
    aktiv                         BOOLEAN DEFAULT TRUE NOT NULL,
    opprettet_tid                 TIMESTAMP(3)         NOT NULL
);

CREATE INDEX idx_stonadsperiode_vurdering_vurderinger_id ON stonadsperiode_vurdering (stonadsperiode_vurderinger_id);

CREATE UNIQUE INDEX idx_stonadsperiode_grunnlag_behandling_aktiv
    ON stonadsperiode_grunnlag (behandling_id)
    WHERE (aktiv = true);
