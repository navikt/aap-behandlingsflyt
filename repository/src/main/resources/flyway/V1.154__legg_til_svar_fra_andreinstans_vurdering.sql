create table svar_fra_andreinstans_vurdering
(
    id                         bigserial                              not null primary key,
    begrunnelse                text                                   not null,
    konsekvens                 varchar(20)                            not null,
    vilkaar_som_skal_omgjoeres TEXT[]       DEFAULT ARRAY []::TEXT[]  not null,
    opprettet_tid              TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av                 varchar(50)                            not null
);

CREATE TABLE svar_fra_andreinstans_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES svar_fra_andreinstans_vurdering (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_SVAR_FRA_ANDREINSTANS_GRUNNLAG_BEHANDLING_ID ON svar_fra_andreinstans_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);