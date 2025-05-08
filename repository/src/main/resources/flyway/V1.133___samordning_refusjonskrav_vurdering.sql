CREATE TABLE TJENESTEPENSJON_REFUSJONSKRAV_VURDERING(
    ID                              BIGSERIAL       NOT NULL   PRIMARY KEY,
    HAR_KRAV                        BOOLEAN         NOT NULL,
    FOM                             DATE            NULL,
    TOM                             DATE            NULL,
    OPPRETTET_TID                   TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG(
    ID                              BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID                   BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    SAK_ID                          BIGINT                                 NOT NULL REFERENCES SAK (ID),
    REFUSJONKRAV_VURDERING_ID       BIGINT                                 NOT NULL REFERENCES TJENESTEPENSJON_REFUSJONSKRAV_VURDERING (ID),
    AKTIV                           BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);