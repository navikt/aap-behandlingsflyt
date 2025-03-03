-- automatisk generert basert på pliktkort-tabellene

create table meldekortene
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null
);

create table meldekort
(
    id              bigserial primary key,
    meldekortene_id bigint                                 not null references meldekortene,
    journalpost     varchar(25)                            not null,
    opprettet_tid   timestamp(3) default CURRENT_TIMESTAMP not null
);

create index idx_meldekort on meldekort (journalpost);

create table meldekort_periode
(
    id            bigserial primary key,
    meldekort_id  bigint                                 not null references meldekort,
    periode       daterange                              not null,
    timer_arbeid  numeric(5, 1)                          not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    constraint meldekort_periode_ikke_overlapp_periode exclude using gist (meldekort_id with =, periode with &&)
);

create table meldekort_grunnlag
(
    id              bigserial primary key,
    behandling_id   bigint                                 not null references behandling,
    meldekortene_id bigint                                 not null references meldekortene,
    aktiv           boolean      default true              not null,
    opprettet_tid   timestamp(3) default CURRENT_TIMESTAMP not null
);

create unique index uidx_meldekort_grunnlag_behandling_id
    on meldekort_grunnlag (behandling_id)
    where (aktiv = true);
