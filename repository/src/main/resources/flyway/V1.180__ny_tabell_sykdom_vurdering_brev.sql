CREATE TABLE SYKDOM_VURDERING_BREV
(
    ID                   BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID        BIGINT                                 NULL REFERENCES BEHANDLING (ID) UNIQUE,
    VURDERING            TEXT                                   NULL,
    VURDERT_AV           TEXT DEFAULT 'Ukjent'                  NOT NULL,
    OPPRETTET_TID        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);