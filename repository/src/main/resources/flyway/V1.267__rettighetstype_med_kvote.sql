create table rettighetstype_perioder
(
    id            bigserial primary key,
    opprettet_tid timestamp default current_timestamp not null
);

create table rettighetstype_grunnlag
(
    id            bigserial primary key,
    perioder_id   bigint references rettighetstype_perioder (id) not null,
    behandling_id bigint                                         not null references behandling (id),
    opprettet_tid timestamp default current_timestamp            not null,
    aktiv         boolean   default true                         not null
);

create table rettighetstype_periode
(
    id                    bigserial primary key,
    perioder_id           bigint references rettighetstype_perioder (id) not null,
    periode               daterange                                      not null,
    rettighetstype        text                                           not null,
    bruker_av_kvoter      text[],
    avslagsaarsaker_kvote text[],
    opprettet_tid         timestamp default current_timestamp            not null
);

create unique index uidx_rettighetstype_grunnlag_behandling_id on rettighetstype_grunnlag (behandling_id) where (aktiv = true); 


