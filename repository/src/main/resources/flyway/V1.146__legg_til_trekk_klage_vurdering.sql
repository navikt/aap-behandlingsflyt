create table trekk_klage_vurdering
(
    id                      serial                                 not null primary key,
    skal_trekkes            BOOLEAN                                not null,
    hvorfor_trekkes         varchar(50),
    begrunnelse             TEXT                                   not null,
    opprettet_tid           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av              varchar(50)                            not null
);

CREATE TABLE trekk_klage_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES TREKK_KLAGE_VURDERING (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_TRUKKET_KLAGE_GRUNNLAG_BEHANDLING_ID ON trekk_klage_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);
