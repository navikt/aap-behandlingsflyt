create table trukket_soknad_vurderinger
(
    id        bigserial primary key,
    opprettet timestamp(3) not null default current_timestamp(3)
);

create table trukket_soknad_vurdering
(
    id             bigserial primary key,
    vurderinger_id bigint       not null references trukket_soknad_vurderinger (id),
    begrunnelse    text         not null,
    journalpost_id text         not null,
    vurdert_av     text         not null,
    vurdert        timestamp(3) not null,
    opprettet      timestamp(3) not null default current_timestamp(3)
);


create table trukket_soknad_grunnlag
(
    id             bigserial primary key,
    behandling_id  bigint       not null references behandling (id),
    vurderinger_id bigint       not null references trukket_soknad_vurderinger (id),
    opprettet      timestamp(3) not null default current_timestamp(3),
    aktiv          boolean      not null default true
);

create unique index uidx_trukket_soknad_grunnlag on
    trukket_soknad_grunnlag (behandling_id) where aktiv;