CREATE TABLE MARKERING
(
    ID                   BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID        BIGINT                                 NULL REFERENCES BEHANDLING (ID),
    MARKERING_TYPE       TEXT                                   NULL,
    ER_AKTIV             BOOLEAN DEFAULT FALSE                  NOT NULL,
    BEGRUNNELSE          TEXT                                   NOT NULL,
    OPPRETTET_TID        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    OPPRETTET_AV         TEXT DEFAULT 'Ukjent'                  NOT NULL
);