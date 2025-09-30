with to_fill as (select v.id, opprettet_tid
                 from sykepenge_vurdering v
                 where v.vurderinger_id is null),
     ins as (
         insert into sykepenge_vurderinger (opprettet_tid)
             select opprettet_tid
             from to_fill
             returning id as vurderinger_id),
     n1 as (select id as vurdering_id, row_number() over (order by id) rn
            from to_fill),
     n2 as (select vurderinger_id, row_number() over (order by vurderinger_id) rn
            from ins),
     map as (select n1.vurdering_id, n2.vurderinger_id
             from n1
                      join n2 using (rn))
update sykepenge_vurdering v
set vurderinger_id = m.vurderinger_id
from map m
where v.id = m.vurdering_id;



update sykepenge_erstatning_grunnlag g
set vurderinger_id = v.vurderinger_id
from sykepenge_vurdering v
where g.vurdering_id = v.id
  and g.vurderinger_id is null;