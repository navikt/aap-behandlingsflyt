create table avbryt_aktivitetspliktbehandling_vurdering
(
    id              bigserial       primary key,
    aarsak          text            not null,
    begrunnelse     text            not null,
    vurdert_av      text            not null,
    opprettet_tid   timestamptz     not null default now()
);

create table avbryt_aktivitetspliktbehandling_grunnlag
(
    id              bigserial     primary key,
    behandling_id   bigint        not null references behandling (id),
    vurdering_id    bigint        references avbryt_aktivitetspliktbehandling_vurdering (id),
    aktiv           boolean       not null default true,
    opprettet_tid   timestamptz   not null default now()
);

create unique index uidx_avbryt_aktivitetspliktbehandling_grunnlag_behandling_id
    on avbryt_aktivitetspliktbehandling_grunnlag (behandling_id)
    where (aktiv = true);