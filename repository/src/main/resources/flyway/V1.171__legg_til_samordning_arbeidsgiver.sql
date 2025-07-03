CREATE TABLE SAMORDNING_ARBEIDSGIVER_VURDERING(
                                       ID                              BIGSERIAL       NOT NULL   PRIMARY KEY,
                                       VURDERING                       TEXT            NULL,
                                       FOM                             DATE            NOT NULL,
                                       TOM                             DATE            NOT NULL,
                                       OPPRETTET_TID                   TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_SAMORDNING_ARBEIDSGIVER_VURDERING_ID ON SAMORDNING_ARBEIDSGIVER_VURDERING (ID);

CREATE TABLE SAMORDNING_ARBEIDSGIVER_GRUNNLAG(
                                      ID                              BIGSERIAL                              NOT NULL PRIMARY KEY,
                                      BEHANDLING_ID                   BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
                                      SAK_ID                          BIGINT                                 NOT NULL REFERENCES SAK (ID),
                                      SAMORDNING_ARBEIDSGIVER_VURDERING_ID       BIGINT                                 NOT NULL REFERENCES SAMORDNING_ARBEIDSGIVER_VURDERING (ID),
                                      AKTIV                           BOOLEAN      DEFAULT TRUE              NOT NULL,
                                      OPPRETTET_TID                   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);