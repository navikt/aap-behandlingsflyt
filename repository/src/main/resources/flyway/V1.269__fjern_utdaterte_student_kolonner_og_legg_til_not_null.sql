alter table student_grunnlag drop column student_id;

alter table student_vurdering
    alter column fom set not null,
    alter column vurdert_i_behandling set not null,
    alter column student_vurderinger_id set not null;