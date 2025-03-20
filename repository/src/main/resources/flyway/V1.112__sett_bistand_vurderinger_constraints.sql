alter table bistand_grunnlag 
    alter column bistand_id drop not null,
    alter column bistand_vurderinger_id set not null;

alter table bistand
    alter column bistand_vurderinger_id set not null;
    