create table stans_opphor_vurderinger
(
    id bigserial primary key,
    opprettet_tid timestamp(3) not null
);

create table stans_opphor_grunnlag
(
    id bigserial primary key,
    behandling_id bigint not null references behandling(id),
    vurderinger_id bigint not null references stans_opphor_vurderinger(id),
    opprettet_tid timestamp(3) not null,
    aktiv boolean not null
);

create table stans_opphor_vurdering
(
    id bigserial primary key,
    vurderinger_id bigint not null references stans_opphor_vurderinger(id),
    opprettet_tid timestamp(3) not null,

    vedtaksstatus text not null,
    fom date not null,
    vurdert_i_behandling bigint not null references behandling(id),

    -- Kun for 'GJELDENDE'
    vedtakstype text null check (vedtaksstatus <> 'GJELDENDE' or vedtakstype in ('STANS', 'OPPHØR')),
    avslagsaarsaker text[] null check (vedtaksstatus <> 'GJELDENDE' or avslagsaarsaker is not null)
);
