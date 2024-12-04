CREATE TABLE HELSEINSTITUSJON_GRUNNLAG
(
    ID                                     BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID                          BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    FAAR_KOST_OG_LOSJI                     BOOLEAN                                NOT NULL,
    BEGRUNNELSE                            TEXT                                   NOT NULL,
    HAR_FASTE_UTGIFTER                     BOOLEAN,
    FORSOERGER_EKTEFELLE                   BOOLEAN,
    AKTIV                                  BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID                          TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_HELSEINSTITUSJON_GRUNNLAG_HISTORIKK ON HELSEINSTITUSJON_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);
