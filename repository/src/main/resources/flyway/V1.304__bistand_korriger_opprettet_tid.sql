update bistand
set opprettet_tid = bg.opprettet_tid
from bistand_grunnlag bg
         inner join behandling ON behandling.id = bg.behandling_id
         inner join sak ON sak.id = behandling.sak_id
where 
    bistand.bistand_vurderinger_id in (select bistand_vurderinger_id from bistand_grunnlag where aktiv)
  and bg.aktiv = true
  and bg.behandling_id = bistand.vurdert_i_behandling
  and saksnummer in ('LoCAL_4LEMD1K'); -- Sett dette til noe fornuftig