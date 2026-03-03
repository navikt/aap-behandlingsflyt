DROP index uidx_vurderingsbehov;

ALTER TABLE vurderingsbehov
    drop column periode;

delete
from vurderingsbehov
where id in (select id
             from (select *,
                          row_number()
                          over (partition by behandling_id, aarsak order by id desc) as rn
                   from vurderingsbehov) as vurderingsbehov_rn
             where rn > 1);

CREATE UNIQUE INDEX UIDX_AARSAK_TIL_BEHANDLING ON vurderingsbehov (BEHANDLING_ID, AARSAK);