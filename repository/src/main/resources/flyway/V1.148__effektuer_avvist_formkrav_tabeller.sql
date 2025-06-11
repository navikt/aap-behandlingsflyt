create table avvist_formkrav_varsel
(
    id             bigserial primary key,
    dato_varslet   date,
    frist          date,
    brev_referanse UUID                                   not null,
    opprettet_tid  timestamp(3) default current_timestamp not null
);

create table effektuer_avvist_formkrav_vurdering
(
    id                   bigserial primary key,
    skal_endelig_avvises boolean                                not null,
    opprettet_tid        timestamp(3) default current_timestamp not null
);

create table effektuer_avvist_formkrav_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                                 not null references behandling (id),
    varsel_id     bigint                                 not null references avvist_formkrav_varsel (id),
    vurdering_id  bigint references effektuer_avvist_formkrav_vurdering (id),
    aktiv         boolean      default true              not null,
    opprettet_tid timestamp(3) default current_timestamp not null
);


create unique index idx_effektuer_avvist_formkrav_grunnlag_behandling_id
    on effektuer_avvist_formkrav_grunnlag (behandling_id)
    where (aktiv = true);
