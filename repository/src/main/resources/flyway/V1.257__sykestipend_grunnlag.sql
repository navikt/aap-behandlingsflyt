create table sykestipend_vurdering
(
    id                   bigserial primary key,
    begrunnelse          text   not null,
    perioder             daterange[],
    vurdert_i_behandling bigint not null references behandling (id),
    vurdert_av_ident     text   not null,
    opprettet            timestamp default current_timestamp
);

create table sykestipend_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                 not null references behandling (id),
    opprettet     timestamp default current_timestamp,
    aktiv         boolean   default true not null,
    vurdering_id  bigint                 not null references sykestipend_vurdering (id)
);

create unique index uidx_sykestipend_grunnlag_behandling_id on sykestipend_grunnlag (behandling_id) where (aktiv = true);
