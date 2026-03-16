-- Koblingstabell for vedtakslengde vurderinger
create table vedtakslengde_vurderinger
(
    id        bigserial primary key,
    opprettet timestamp(3) default current_timestamp not null
);

-- Legg til vurderinger_id i vedtakslengde_vurdering
alter table vedtakslengde_vurdering add column vurderinger_id bigint references vedtakslengde_vurderinger(id);

-- Legg til vurderinger_id i vedtakslengde_grunnlag
alter table vedtakslengde_grunnlag add column vurderinger_id bigint references vedtakslengde_vurderinger(id);

-- Migrer eksisterende data.
-- Grunnlag som deler samme vurdering_id (via kopier) skal dele samme vurderinger_id.
do $$
declare
    rec record;
    ny_vurderinger_id bigint;
begin
    for rec in select distinct vurdering_id
               from vedtakslengde_grunnlag
               where vurdering_id is not null
    loop
        insert into vedtakslengde_vurderinger default values returning id into ny_vurderinger_id;
        update vedtakslengde_vurdering set vurderinger_id = ny_vurderinger_id where id = rec.vurdering_id;
        update vedtakslengde_grunnlag set vurderinger_id = ny_vurderinger_id where vurdering_id = rec.vurdering_id;
    end loop;
end $$;

-- Fjern gammel vurdering_id kolonne fra grunnlag
alter table vedtakslengde_grunnlag drop column vurdering_id;

-- Sett NOT NULL constraint
alter table vedtakslengde_vurdering alter column vurderinger_id set not null;
alter table vedtakslengde_grunnlag alter column vurderinger_id set not null;
