create table rettighetsperiode_vurderinger
(
    id        bigserial primary key,
    opprettet timestamp(3) not null default current_timestamp(3)
);

create table rettighetsperiode_vurdering
(
    id             bigserial primary key,
    vurderinger_id bigint       not null references rettighetsperiode_vurderinger (id),
    begrunnelse    text         not null,
    start_dato     date         not null,
    aarsak         text         not null,
    opprettet      timestamp(3) not null default current_timestamp(3)
);


create table rettighetsperiode_grunnlag
(
    id             bigserial primary key,
    behandling_id  bigint       not null references behandling (id),
    vurderinger_id bigint       not null references rettighetsperiode_vurderinger (id),
    opprettet      timestamp(3) not null default current_timestamp(3),
    aktiv          boolean      not null default true
);

create unique index uidx_rettighetsperiode_grunnlag on
    rettighetsperiode_grunnlag (behandling_id) where aktiv;