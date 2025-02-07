alter table sykdom_grunnlag
    drop column sykdom_id,
    alter column sykdom_vurderinger_id set not null;

-- disse er kopiert over til nytt format, så ingen vurderinger blir tapt
delete from sykdom_vurdering
    where sykdom_vurderinger_id is null;

alter table sykdom_vurdering
    alter column sykdom_vurderinger_id set not null,
    alter column opprettet_tid set not null;