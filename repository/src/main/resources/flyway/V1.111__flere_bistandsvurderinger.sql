create table bistand_vurderinger
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp
);

alter table bistand
    add column bistand_vurderinger_id bigint references bistand_vurderinger (id);

alter table bistand_grunnlag
    add column bistand_vurderinger_id bigint references bistand_vurderinger (id);