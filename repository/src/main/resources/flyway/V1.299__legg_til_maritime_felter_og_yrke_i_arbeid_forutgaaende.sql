CREATE TABLE ARBEID_DETALJER
(
    ID                          BIGSERIAL                              NOT NULL PRIMARY KEY,
    ARBEID_FORUTGAAENDE_ID      BIGINT                                 NOT NULL REFERENCES ARBEID_FORUTGAAENDE (ID),
    SKIPSREGISTER_KODE          TEXT                                   NULL,
    SKIPSREGISTER_BESKRIVELSE   TEXT                                   NULL,
    SKIPSTYPE_KODE              TEXT                                   NULL,
    SKIPSTYPE_BESKRIVELSE       TEXT                                   NULL,
    FARTSOMRAADE_KODE           TEXT                                   NULL,
    FARTSOMRAADE_BESKRIVELSE    TEXT                                   NULL,
    YRKE_KODE                   TEXT                                   NULL,
    YRKE_BESKRIVELSE            TEXT                                   NULL,
    OPPRETTET_TID               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IDX_ARBEID_DETALJER_ARBEID_FORUTGAAENDE_ID ON ARBEID_DETALJER (ARBEID_FORUTGAAENDE_ID);
