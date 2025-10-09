create table reduksjon_11_9_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint                                 not null references behandling (id),
    aktiv         boolean      default true              not null,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create table reduksjon_11_9
(
    id                                      bigserial primary key,
    dato                                    date                                                           not null,
    dagsats                                 numeric(21, 0)                                                 not null,
    reduksjon_11_9_grunnlag_id bigint references reduksjon_11_9_grunnlag (id) not null
);


create unique index reduksjon_11_9_grunnlag_behandling_uindex
    on reduksjon_11_9_grunnlag (behandling_id) where aktiv;