create table prosessert_11_7_vurdering
(
    id                            serial primary key,
    prosessert_i_behandling_id    bigint    not null references behandling (id),
    aktivitetsplikt_behandling_id bigint    not null references behandling (id),
    opprettet_tid                 timestamp not null default CURRENT_TIMESTAMP(3)
);

create index idx_prosesserte_11_7_vurderinger_aktivitetsplikt_behandling_id
    on prosessert_11_7_vurdering (prosessert_i_behandling_id);