CREATE TABLE ETABLERING_EGEN_VIRKSOMHET_VURDERINGER
(
    ID            BIGSERIAL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3)                           NOT NULL
);

CREATE TABLE EGEN_VIRKSOMHET_UTVIKLING_PERIODER
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3)                           NOT NULL
);

CREATE TABLE EGEN_VIRKSOMHET_OPPSTART_PERIODER
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3)                           NOT NULL
);

CREATE TABLE ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG
(
    ID             BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID  BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERINGER_ID BIGINT NULL REFERENCES ETABLERING_EGEN_VIRKSOMHET_VURDERINGER (ID),
    AKTIV          BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3)                           NOT NULL
);
CREATE UNIQUE INDEX IDX_ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG_BEHANDLING_ID ON ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);

CREATE TABLE ETABLERING_EGEN_VIRKSOMHET_VURDERING
(
    ID                                    BIGSERIAL PRIMARY KEY,
    BEGRUNNELSE                           TEXT                                   NOT NULL,
    FORELIGGER_FAGLIG_VURDERING           BOOLEAN                                NOT NULL,
    VIRKSOMHET_ER_NY                      BOOLEAN                                NULL,
    BRUKER_EIER_VIRKSOMHET                TEXT                                   NULL,
    KAN_BLI_SELVFORSORGET                 BOOLEAN                                NULL,
    VIRKSOMHET_NAVN                       TEXT                                   NOT NULL,
    ORG_NR                                TEXT   NULL,
    EGEN_VIRKSOMHET_UTVIKLING_PERIODER_ID BIGINT NULL REFERENCES EGEN_VIRKSOMHET_UTVIKLING_PERIODER (ID),
    EGEN_VIRKSOMHET_OPPSTART_PERIODER_ID  BIGINT NULL REFERENCES EGEN_VIRKSOMHET_OPPSTART_PERIODER (ID),
    VURDERINGER_ID                        BIGINT                                 NOT NULL REFERENCES ETABLERING_EGEN_VIRKSOMHET_VURDERINGER (ID),
    VURDERT_I_BEHANDLING                  BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERT_AV                            varchar(50)                            NOT NULL,
    GJELDER_FRA                           DATE                                   NOT NULL,
    GJELDER_TIL                           DATE NULL,
    OPPRETTET_TID                         TIMESTAMP(3)                           NOT NULL
);
CREATE INDEX IDX_ETABLERING_EGEN_VIRKSOMHET_VURDERING_VURDERINGER_ID ON ETABLERING_EGEN_VIRKSOMHET_VURDERING (VURDERINGER_ID);

CREATE TABLE EGEN_VIRKSOMHET_UTVIKLING_PERIODE
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    PERIODER_ID   BIGINT                                 NOT NULL REFERENCES EGEN_VIRKSOMHET_UTVIKLING_PERIODER (ID),
    PERIODE       DATERANGE                              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3)                           NOT NULL
);

CREATE TABLE EGEN_VIRKSOMHET_OPPSTART_PERIODE
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    PERIODER_ID   BIGINT                                 NOT NULL REFERENCES EGEN_VIRKSOMHET_OPPSTART_PERIODER (ID),
    PERIODE       DATERANGE                              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3)                           NOT NULL
);