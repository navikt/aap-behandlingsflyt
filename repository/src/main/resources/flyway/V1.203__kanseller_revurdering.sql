create table kanseller_revurdering_vurdering
(
    id              bigserial       primary key,
    aarsak          varchar(50),
    begrunnelse     text            not null,
    vurdert_av      text            not null,
    opprettet_tid   timestamp(3)    not null default current_timestamp(3)
);

create table kanseller_revurdering_grunnlag
(
    id              bigserial     primary key,
    behandling_id   bigint        not null references behandling (id),
    vurdering_id    bigint        references kanseller_revurdering_vurdering (id),
    aktiv           boolean       not null default true,
    opprettet_tid   timestamp(3)  not null default current_timestamp(3)
);

create unique index uidx_kanseller_revurdering_grunnlag_behandling_id
    on kanseller_revurdering_grunnlag (behandling_id)
    where (aktiv = true);