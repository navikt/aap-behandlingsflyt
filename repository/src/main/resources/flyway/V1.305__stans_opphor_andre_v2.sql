create table stans_opphor_set(
    id bigserial primary key,
    opprettet_tid timestamp(3) not null
);

create table stans_opphor(
    id bigserial primary key,
    stans_opphor_set_id bigint references stans_opphor_set(id),
    fom date not null,
    vurdering text not null,
    avslagsaarsaker text[] not null
);

create table stans_opphor_vurderinger_v2(
    id bigserial primary key,
    opprettet_tid timestamp(3) not null
);

create table stans_opphor_vurdering_v2(
    id bigserial primary key,
    vurderinger_id bigint not null references stans_opphor_vurderinger_v2(id),
    vurdert_tidspunkt timestamp(3) not null,
    vurdert_i_behandling bigint not null references behandling(id),
    fom date not null,
    vurdering text not null,
    avslagsaarsaker text[]
);

alter table stans_opphor_grunnlag
add column stans_opphor_set_id bigint references stans_opphor_set(id),
add column vurderinger_id_v2 bigint references stans_opphor_vurderinger_v2(id);