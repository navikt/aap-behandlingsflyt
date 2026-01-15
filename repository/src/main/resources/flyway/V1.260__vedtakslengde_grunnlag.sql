create table vedtakslengde_vurdering
(
    id                   bigserial primary key,
    sluttdato            date   not null,
    utvidet_med          text   not null,
    vurdert_av           text   not null,
    vurdert_i_behandling bigint not null references behandling (id),
    opprettet            timestamp default current_timestamp
);

create table vedtakslengde_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                 not null references behandling (id),
    opprettet     timestamp default current_timestamp,
    aktiv         boolean   default true not null,
    vurdering_id  bigint                 not null references vedtakslengde_vurdering(id)
);

create unique index uidx_vedtakslengde_grunnlag_behandling_id on vedtakslengde_grunnlag (behandling_id) where (aktiv = true);