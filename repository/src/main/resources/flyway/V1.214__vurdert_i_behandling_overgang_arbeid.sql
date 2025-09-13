drop table overgang_arbeid_vurdering;

create table overgang_arbeid_vurdering
(
    id                      bigserial primary key,
    begrunnelse             text                                   not null,
    bruker_rett_paa_aap     boolean                                not null,
    vurdert_av              VARCHAR(50)                            not null,
    vurderingen_gjelder_fra date                                   not null,
    vurderingen_gjelder_til date                                   null,
    opprettet_tid           timestamp(3)                           not null,
    vurderinger_id          bigint                                 not null references overgang_arbeid_vurderinger (id),
    vurdert_i_behandling    bigint                                 not null references behandling (id)
);


