create table fullmektig_vurdering(
    id bigserial not null primary key,
    har_fullmektig boolean not null,
    fullmektig_ident varchar(19),
    fullmektig_navn_og_adresse jsonb,
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null,
    vurdert_av varchar(50) not null
);

create table fullmektig_grunnlag(
    id bigserial not null primary key,
    behandling_id bigint not null references behandling(id),
    vurdering_id bigint null references fullmektig_vurdering(id),
    aktiv boolean default true not null,
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP not null
);

create unique index uidx_fullmektig_grunnlag_behandling_id on fullmektig_grunnlag(behandling_id) where (aktiv = true);
