DROP TABLE TJENESTEPENSJON_YTELSE;
DROP TABLE TJENESTEPENSJON_GRUNNLAG;
DROP TABLE TJENESTEPENSJON_YTELSER;

CREATE TABLE TJENESTEPENSJON_ORDNINGER
(
    ID            BIGSERIAL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE TJENESTEPENSJON_FORHOLD_GRUNNLAG
(
    ID                           BIGSERIAL PRIMARY KEY,
    AKTIV                        bool                                   NOT NULL,
    BEHANDLING_ID                BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    OPPRETTET_TID                TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    TJENESTEPENSJON_ORDNINGER_ID BIGINT                                 NOT NULL REFERENCES TJENESTEPENSJON_ORDNINGER (ID)
);


CREATE TABLE TJENESTEPENSJON_ORDNING
(
    ID                           BIGSERIAL PRIMARY KEY,
    TJENESTEPENSJON_ORDNINGER_ID BIGINT NOT NULL REFERENCES TJENESTEPENSJON_ORDNINGER (ID),
    samtykkeSimulering           BOOL   NOT NULL,
    harUtlandsPensjon            BOOL   NOT NULL,
    datoSamtykkeGitt             DATE,
    navn                         TEXT   NOT NULL,
    tpNr                         TEXT   NOT NULL,
    orgNr                        TEXT   NOT NULL,
    tssId                        TEXT   NOT NULL,
    harSimulering                BOOL   NOT NULL,
    kilde                        TEXT   NOT NULL
);

CREATE TABLE TJENESTEPENSJON_YTELSE_2
(
    ID                         BIGSERIAL PRIMARY KEY,
    ytelseId_extern            BIGINT NOT NULL,
    TJENESTEPENSJON_ORDNING_ID BIGINT NOT NULL REFERENCES TJENESTEPENSJON_ORDNING (ID),
    YTELSE_TYPE                TEXT   NOT NULL,
    INNMELDT_FOM               DATE,
    IVERKSATT_FOM              DATE   NOT NULL,
    IVERKSATT_TOM              DATE
)
