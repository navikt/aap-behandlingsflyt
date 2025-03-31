create table informasjonskrav_oppdatert
(
    id               bigserial primary key,
    sak_id           bigint       not null references sak (id),
    behandling_id    bigint       not null references behandling (id),
    oppdatert        timestamp(3) not null,
    informasjonskrav text         not null
);

create index idx_informasjonskrav_oppdatert_sak_id_oppdatert_krav on
    informasjonskrav_oppdatert (sak_id, oppdatert, informasjonskrav);