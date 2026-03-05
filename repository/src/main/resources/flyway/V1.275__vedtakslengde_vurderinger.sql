-- Koblingstabell for vedtakslengde vurderinger
create table vedtakslengde_vurderinger
(
    id        bigserial primary key,
    opprettet timestamp default current_timestamp
);

-- Legg til vurderinger_id i vedtakslengde_vurdering
alter table vedtakslengde_vurdering add column vurderinger_id bigint references vedtakslengde_vurderinger(id);

-- Legg til vurderinger_id i vedtakslengde_grunnlag
alter table vedtakslengde_grunnlag add column vurderinger_id bigint references vedtakslengde_vurderinger(id);

-- Migrer eksisterende data: opprett en rad i vedtakslengde_vurderinger per eksisterende grunnlag
-- og koble sammen vurdering og grunnlag via den nye koblingstabellen
do $$
declare
    rec record;
    ny_vurderinger_id bigint;
begin
    for rec in select g.id as grunnlag_id, g.vurdering_id
               from vedtakslengde_grunnlag g
               where g.vurdering_id is not null
    loop
        insert into vedtakslengde_vurderinger default values returning id into ny_vurderinger_id;
        update vedtakslengde_vurdering set vurderinger_id = ny_vurderinger_id where id = rec.vurdering_id;
        update vedtakslengde_grunnlag set vurderinger_id = ny_vurderinger_id where id = rec.grunnlag_id;
    end loop;
end $$;

-- Fjern gammel vurdering_id kolonne fra grunnlag
alter table vedtakslengde_grunnlag drop column vurdering_id;

-- Sett NOT NULL constraint
alter table vedtakslengde_vurdering alter column vurderinger_id set not null;
alter table vedtakslengde_grunnlag alter column vurderinger_id set not null;

-- Gjenopprett unik indeks
drop index if exists uidx_vedtakslengde_grunnlag_behandling_id;
create unique index uidx_vedtakslengde_grunnlag_behandling_id on vedtakslengde_grunnlag (behandling_id) where (aktiv = true);

