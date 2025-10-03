CREATE TABLE sykepenge_vurderinger
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp
);

ALTER TABLE sykepenge_vurdering
    add column vurderinger_id bigint references sykepenge_vurderinger;

alter table sykepenge_erstatning_grunnlag
    add column vurderinger_id bigint references sykepenge_vurderinger;