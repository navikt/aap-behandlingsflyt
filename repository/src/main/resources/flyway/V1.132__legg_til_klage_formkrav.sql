create table formkrav_vurdering
(
    id                 serial primary key,
    begrunnelse        text                                   not null,
    er_bruker_part     boolean                                not null,
    er_frist_overholdt boolean                                not null,
    er_konkret         boolean                                not null,
    er_signert         boolean                                not null,
    opprettet_tid      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vurdert_av         varchar(50)                            not null
);

CREATE TABLE formkrav_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES FORMKRAV_VURDERING (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);