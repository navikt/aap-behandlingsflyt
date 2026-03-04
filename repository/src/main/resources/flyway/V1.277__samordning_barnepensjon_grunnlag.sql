create table samordning_barnepensjon_vurdering
(
    id                   bigserial primary key,
    begrunnelse          text   not null,
    vurdert_i_behandling bigint not null references behandling (id),
    vurdert_av_ident     text   not null,
    opprettet            timestamp default current_timestamp
);

create table samordning_barnepensjon_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                 not null references behandling (id),
    opprettet     timestamp default current_timestamp,
    aktiv         boolean   default true not null,
    vurdering_id  bigint                 not null references samordning_barnepensjon_vurdering (id)
);

create table samordning_barnepensjon_vurdering_periode
(
    id            bigserial primary key,
    vurdering_id  bigint    not null references samordning_barnepensjon_vurdering (id),
    periode       daterange not null,
    maaned_beloep numeric   not null,
    constraint samordning_barnepensjon_periode_ikke_overlapp_periode exclude using gist (
        vurdering_id with =,periode with &&)
);

