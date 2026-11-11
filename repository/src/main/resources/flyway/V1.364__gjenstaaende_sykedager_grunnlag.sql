CREATE TABLE gjenstaaende_sykedager_grunnlag
(
    id                 BIGSERIAL PRIMARY KEY,
    behandling_id      BIGINT       NOT NULL REFERENCES behandling (id),
    aktiv              BOOLEAN      NOT NULL DEFAULT TRUE,
    grunnlag           VARCHAR(100) NOT NULL,
    gjenstaaende_dager INT,
    vurdert_til_og_med DATE,
    opprettet_tid      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uidx_gjenstaaende_sykedager_grunnlag_behandling_id
    ON gjenstaaende_sykedager_grunnlag (behandling_id) WHERE (aktiv = TRUE);
