CREATE TABLE migreringsdato_vurderinger (
    id BIGSERIAL NOT NULL PRIMARY KEY
);

CREATE TABLE migreringsdato_vurdering (
    id                   BIGSERIAL    NOT NULL PRIMARY KEY,
    migreringsdato       DATE         NOT NULL,
    vurdert_av           TEXT         NOT NULL,
    vurdert_i_behandling BIGINT       NOT NULL REFERENCES behandling (id),
    opprettet            TIMESTAMP(3) NOT NULL,
    vurderinger_id       BIGINT       NOT NULL REFERENCES migreringsdato_vurderinger (id)
);

CREATE TABLE migreringsdato_grunnlag (
    id             BIGSERIAL            NOT NULL PRIMARY KEY,
    behandling_id  BIGINT               NOT NULL REFERENCES behandling (id),
    vurderinger_id BIGINT               NOT NULL REFERENCES migreringsdato_vurderinger (id),
    aktiv          BOOLEAN DEFAULT TRUE NOT NULL,
    opprettet      TIMESTAMP(3)         NOT NULL
);

CREATE UNIQUE INDEX uidx_migreringsdato_grunnlag_behandling_id
    ON migreringsdato_grunnlag (behandling_id) WHERE (aktiv = true);
