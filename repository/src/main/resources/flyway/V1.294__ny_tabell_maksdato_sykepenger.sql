create table sykepenge_maksdato (
    id bigserial primary key,
    person_id bigserial not null unique references person (id),
    maksdato date not null,
    kilde text not null,
    opprettet_tid timestamptz not null default now()
);