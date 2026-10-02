CREATE TABLE sam_varsling
(
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    behandling_id            BIGINT       NOT NULL REFERENCES behandling (id),
    vedtak_id                BIGINT       NOT NULL,
    varslet                  BOOLEAN      NOT NULL,
    forstegangsbehandling    BOOLEAN      NOT NULL,
    endring_i_rettighetstype BOOLEAN      NOT NULL,
    antall_tp_ytelser        INTEGER      NOT NULL,
    request                  JSONB        NOT NULL,
    vent_paa_svar            BOOLEAN,
    opprettet_tid            TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sam_varsling_behandling_id ON sam_varsling (behandling_id);
