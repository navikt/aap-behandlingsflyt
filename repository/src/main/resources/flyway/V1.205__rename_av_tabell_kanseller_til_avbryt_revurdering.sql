-- Rename tabeller
alter table kanseller_revurdering_vurdering rename to avbryt_revurdering_vurdering;
alter table kanseller_revurdering_grunnlag rename to avbryt_revurdering_grunnlag;

-- Oppdatere fremmednøkkel
alter table avbryt_revurdering_grunnlag
drop constraint if exists kanseller_revurdering_grunnlag_vurdering_id_fkey,
    add constraint avbryt_revurdering_grunnlag_vurdering_id_fkey
        foreign key (vurdering_id) references avbryt_revurdering_vurdering (id);

-- Oppdatere index med nytt navn
drop index if exists uidx_kanseller_revurdering_grunnlag_behandling_id;
create unique index uidx_avbryt_revurdering_grunnlag_behandling_id
    on avbryt_revurdering_grunnlag (behandling_id)
    where (aktiv = true);

alter table avbryt_revurdering_vurdering
alter column aarsak type varchar(100);
