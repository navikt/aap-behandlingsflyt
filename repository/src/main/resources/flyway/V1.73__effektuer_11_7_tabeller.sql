-- Liste av varsler
create table effektuer_11_7_varslinger
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create table effektuer_11_7_varsel
(
    id            bigserial primary key,
    varslinger_id bigint not null references effektuer_11_7_varslinger (id),
    dato_varslet  date   not null
);
create index effektuer_11_7_varsel_id on effektuer_11_7_varsel (varslinger_id);

create table effektuer_11_7_brudd
(
    id                   bigserial primary key,
    varsel_id            bigint not null references effektuer_11_7_varsel (id),
    underveis_periode_id bigint not null references underveis_periode (id)
);
create index effektuer_11_7_brudd_varsel_id on effektuer_11_7_brudd (varsel_id);

create table effektuer_11_7_vurdering
(
    id            bigserial primary key,
    begrunnelse   text                                   not null,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create table effektuer_11_7_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                                 not null references behandling (id),
    varslinger_id bigint                                 not null references effektuer_11_7_varslinger (id),
    vurdering_id  bigint references effektuer_11_7_vurdering (id),
    aktiv         boolean      default true              not null,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create unique index idx_effektuer_11_7_grunnlag_behandling_id on effektuer_11_7_grunnlag (behandling_id) where (aktiv = true);
