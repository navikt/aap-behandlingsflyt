alter table aktivitetsplikt_11_7_vurdering
    add column vurdert_i_behandling bigint references behandling (id);
alter table aktivitetsplikt_11_7_vurdering
    add column skal_ignorere_varsel_frist boolean default false not null;


create table aktivitetsplikt_11_7_varsel
(
    id             bigserial primary key,
    dato_varslet   date,
    frist          date,
    brev_referanse UUID                                   not null,
    behandling_id  bigint references behandling (id),
    opprettet_tid  timestamp(3) default current_timestamp not null
);