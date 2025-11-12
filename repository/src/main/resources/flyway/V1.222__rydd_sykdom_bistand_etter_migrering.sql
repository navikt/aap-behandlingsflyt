alter table bistand_grunnlag drop column bistand_id;

alter table bistand
alter column vurderingen_gjelder_fra set not null;
alter table bistand
alter column vurdert_i_behandling set not null;

alter table sykdom_vurdering
alter column vurderingen_gjelder_fra set not null;
alter table sykdom_vurdering
alter column vurdert_i_behandling set not null;