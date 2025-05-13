-- legg til manglende unique index på formkrav
CREATE UNIQUE INDEX UIDX_FORMKRAV_GRUNNLAG_BEHANDLING_ID ON formkrav_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);

create table paaklaget_behandling_vurdering
(
    id                      serial                                 not null primary key,
    type_vedtak             varchar(100)                           not null,
    paaklaget_behandling_id bigint references behandling (id)      null,
    opprettet_tid           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av              varchar(50)                            not null
);

CREATE TABLE paaklaget_behandling_grunnlag
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    VURDERING_ID  BIGINT                                 NULL REFERENCES PAAKLAGET_BEHANDLING_VURDERING (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_PAAKLAGET_BEHANDLING_GRUNNLAG_BEHANDLING_ID ON paaklaget_behandling_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);
