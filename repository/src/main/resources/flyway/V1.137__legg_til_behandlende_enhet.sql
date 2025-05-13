create table behandlende_enhet_vurdering
(
    id                      serial                                 not null primary key,
    skal_behandles_av_nay    boolean                                not null,
    skal_behandles_av_kontor boolean                                not null,
    opprettet_tid           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av              varchar(50)                            not null
);

CREATE TABLE behandlende_enhet_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES BEHANDLENDE_ENHET_VURDERING (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_BEHANDLENDE_ENHET_GRUNNLAG_BEHANDLING_ID ON behandlende_enhet_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);
