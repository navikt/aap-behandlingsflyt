create table sykdom_vurderinger (
    id bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp
);

alter table sykdom_vurdering
add column sykdom_vurderinger_id bigint references sykdom_vurderinger(id),
add column opprettet_tid timestamp(3) default current_timestamp;

alter table sykdom_grunnlag
add column sykdom_vurderinger_id bigint references sykdom_vurderinger(id);

