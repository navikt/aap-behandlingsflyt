CREATE TABLE MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON
(
    ID            BIGSERIAL     NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3)  DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG
(
    ID                                       BIGSERIAL                                   NOT NULL PRIMARY KEY,
    BEHANDLING_ID                            BIGINT                                      NOT NULL REFERENCES BEHANDLING (ID),
    MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID BIGINT                                      NOT NULL REFERENCES MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON (ID),
    AKTIV                                    BOOLEAN           DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID                            TIMESTAMP(3)      DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IDX_MEDLEMSKAP_FORUTGAAENDE_UNNTAK_BEHANDLING_ID ON MEDLEMSKAP_FORUTGAAENDE_UNNTAK_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);

CREATE TABLE MEDLEMSKAP_FORUTGAAENDE_UNNTAK
(
    ID                                       BIGSERIAL   NOT NULL PRIMARY KEY,
    MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID BIGINT      NOT NULL REFERENCES MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON (ID),
    STATUS                                   TEXT        NOT NULL,
    STATUS_ARSAK                             TEXT,
    MEDLEM                                   BOOLEAN     NOT NULL,
    PERIODE                                  DATERANGE   NOT NULL,
    GRUNNLAG                                 TEXT        NOT NULL,
    LOVVALG                                  TEXT        NOT NULL,
    HELSEDEL                                 BOOLEAN     NOT NULL,
    LOVVALGSLAND                             varchar(50) NULL
);