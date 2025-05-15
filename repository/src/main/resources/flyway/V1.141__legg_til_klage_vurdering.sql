create table klage_nay_vurdering
(
    id                             serial                                 not null primary key,
    begrunnelse                    text                                   not null,
    notat                          text                                   null,
    innstilling                    varchar(20)                            not null,
    vilkaar_som_skal_omgjoeres     TEXT[]       DEFAULT ARRAY []::TEXT[]  not null,
    vilkaar_som_skal_opprettholdes TEXT[]       DEFAULT ARRAY []::TEXT[]  not null,
    opprettet_tid                  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av                     varchar(50)                            not null
);

CREATE TABLE klage_nay_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES klage_nay_vurdering (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_KLAGE_NAY_GRUNNLAG_BEHANDLING_ID ON klage_nay_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);

create table klage_kontor_vurdering
(
    id                             serial                                 not null primary key,
    begrunnelse                    text                                   not null,
    notat                          text                                   null,
    innstilling                    varchar(20)                            not null,
    vilkaar_som_skal_omgjoeres     TEXT[]       DEFAULT ARRAY []::TEXT[]  not null,
    vilkaar_som_skal_opprettholdes TEXT[]       DEFAULT ARRAY []::TEXT[]  not null,
    opprettet_tid                  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av                     varchar(50)                            not null
);

CREATE TABLE klage_kontor_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES klage_kontor_vurdering (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_KLAGE_KONTOR_GRUNNLAG_BEHANDLING_ID ON klage_kontor_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);

