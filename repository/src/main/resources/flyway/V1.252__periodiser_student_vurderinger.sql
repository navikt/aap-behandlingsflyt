create table student_vurderinger
(
    id            bigserial primary key,
    opprettet_tid timestamp(3) default current_timestamp
);

alter table student_vurdering
    add column student_vurderinger_id bigint references student_vurderinger (id),
    add column vurdert_i_behandling bigint references behandling (id),
    add column fom date,
    add column tom date;

alter table student_grunnlag
    add column student_vurderinger_id bigint references student_vurderinger (id);