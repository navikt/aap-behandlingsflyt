CREATE TABLE BRUDD_AKTIVITETSPLIKT
(
    ID                 BIGSERIAL                              NOT NULL PRIMARY KEY,
    SAKSNUMMER         VARCHAR(20)                            NOT NULL,
    BRUDD              VARCHAR(60)                            NOT NULL,
    PERIODE            DATERANGE                              NOT NULL,
    BEGRUNNELSE        TEXT                                   NOT NULL,
    PARAGRAF           VARCHAR(10)                            NOT NULL,
    OPPRETTET_TID      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_BRUDD_AKTIVITETSPLIKT_SAKSNUMMER ON BRUDD_AKTIVITETSPLIKT (SAKSNUMMER);