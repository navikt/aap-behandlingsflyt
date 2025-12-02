create table tilkjent_ytelse_sporing(
    id bigserial primary key,
    tilkjent_ytelse_id bigint not null references tilkjent_ytelse(id),
    versjon text not null,
    faktagrunnlag text not null
);