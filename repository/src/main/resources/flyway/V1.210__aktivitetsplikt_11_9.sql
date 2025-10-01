create table aktivitetsplikt_11_9_vurderinger
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create table aktivitetsplikt_11_9_vurdering
(
    id                   bigserial primary key,
    dato                 date                                                    not null,
    begrunnelse          text                                                    not null,
    brudd                varchar(50)                                             not null,
    grunn                varchar(50)                                             not null,
    opprettet_tid        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP                  not null,
    vurderinger_id       bigint references aktivitetsplikt_11_9_vurderinger (id) not null,
    vurdert_i_behandling bigint references behandling (id)                       not null,
    vurdert_av           varchar(50)                                             not null
);

create table aktivitetsplikt_11_9_grunnlag
(
    id             bigserial                              not null primary key,
    vurderinger_id bigint references aktivitetsplikt_11_9_vurderinger (id),
    behandling_id  bigint                                 not null references behandling (id),
    aktiv          boolean      default true              not null,
    opprettet_tid  timestamp(3) default current_timestamp not null
);

create unique index aktivitetsplikt_11_9_grunnlag_behandling_uindex
    on aktivitetsplikt_11_9_grunnlag (behandling_id) where aktiv;