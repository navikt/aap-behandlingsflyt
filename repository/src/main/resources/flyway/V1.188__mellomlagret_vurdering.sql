CREATE TABLE MELLOMLAGRET_VURDERING
(
    id                   bigserial primary key,
    behandling_id        bigint       not null references behandling (id),
    avklaringsbehov_kode text         not null,
    data                 JSONB        not null,
    vurdert_av           text         not null,
    vurdert_dato         timestamp(3) not null
)