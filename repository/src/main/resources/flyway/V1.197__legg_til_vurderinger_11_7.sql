create table aktivitetsplikt_11_7_vurderinger
(
    id bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp not null
);

alter table aktivitetsplikt_11_7_vurdering
add column vurderinger_id bigint references aktivitetsplikt_11_7_vurderinger(id);

alter table aktivitetsplikt_11_7_grunnlag
add column vurderinger_id bigint references aktivitetsplikt_11_7_vurderinger(id),
drop column vurdering_id;