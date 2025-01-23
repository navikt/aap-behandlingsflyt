CREATE TABLE ARBEID
(
    ID                      BIGSERIAL                              NOT NULL   PRIMARY KEY,
    IDENTIFIKATOR           VARCHAR(30)                            NOT NULL,
    ARBEIDSFORHOLD_KODE     VARCHAR(50)                            NOT NULL,
    ARBEIDER_ID             BIGINT                                 NOT NULL REFERENCES ARBEIDER (ID),
    STARTDATO               DATE                                   NOT NULL,
    SLUTTDATO               DATE                                   NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
)
CREATE INDEX IDX_ARBEID_ARBEIDER_ID ON ARBEID (ARBEIDER_ID);

-- Aggregering
CREATE TABLE ARBEIDER
(
    ID              BIGSERIAL                              NOT NULL   PRIMARY KEY,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
)

CREATE TABLE INNTEKT_I_NORGE
(
    ID                          BIGSERIAL                              NOT NULL   PRIMARY KEY,
    IDENTIFIKATOR               VARCHAR(30)                            NOT NULL,
    BELOEP                      NUMERIC                                NOT NULL,
    SKATTEMESSIG_BOSATT_LAND    VARCHAR(3)                             NULL,
    OPPTJENINGS_LAND            VARCHAR(3)                             NULL
    INNTEKT_TYPE                VARCHAR(50)                            NULL,
    PERIODE                     DATERANGE                              NOT NULL,
    INNTEKTER_I_NORGE_ID        BIGINT                                 NOT NULL REFERENCES INNTEKTER_I_NORGE (ID),
    OPPRETTET_TID               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
)
CREATE INDEX IDX_INNTEKT_I_NORGE_INNTEKTER_I_NORGE_ID ON INNTEKT_I_NORGE (INNTEKTER_I_NORGE_ID);

-- Aggregering
CREATE TABLE INNTEKTER_I_NORGE
(
    ID              BIGSERIAL                              NOT NULL   PRIMARY KEY,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
)

CREATE TABLE MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
(
    ID                              BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID                   BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    MEDLEMSKAP_UNNTAK_PERSON_ID     BIGINT                                 NULL REFERENCES MEDLEMSKAP_UNNTAK_PERSON (ID),
    INNTEKTER_I_NORGE_ID            BIGINT                                 NULL REFERENCES INNTEKTER_I_NORGE (ID),
    ARBEIDER_ID                     BIGINT                                 NULL REFERENCES ARBEIDER (ID),
    AKTIV                           BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG_BEHANDLING_ID ON MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (BEHANDLING_ID);