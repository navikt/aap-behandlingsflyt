create table aktivitetsplikt_11_7_vurdering
(
    id            serial primary key,
    begrunnelse   text                                   not null,
    er_oppfylt    boolean                                not null,
    utfall        varchar(20),
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av    varchar(50)                            not null
);

create table aktivitetsplikt_11_7_grunnlag
(
    id            bigserial                              not null primary key,
    behandling_id bigint                                 not null references behandling (id),
    vurdering_id  bigint                                 null references aktivitetsplikt_11_7_vurdering (id),
    aktiv         boolean      default true              not null,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create unique index aktivitetsplikt_11_7_grunnlag_behandling_uindex
    on aktivitetsplikt_11_7_grunnlag (behandling_id) where aktiv;