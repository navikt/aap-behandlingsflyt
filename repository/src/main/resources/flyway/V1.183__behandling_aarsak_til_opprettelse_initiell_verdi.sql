update behandling
set aarsak_til_opprettelse = (case
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_SØKNAD' THEN 'SØKNAD'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_AKTIVITETSMELDING' THEN 'AKTIVITETSMELDING'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_MELDEKORT' THEN 'MELDEKORT'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_LEGEERKLÆRING' THEN 'HELSEOPPLYSNINGER'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_AVVIST_LEGEERKLÆRING' THEN 'HELSEOPPLYSNINGER'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_DIALOGMELDING' THEN 'HELSEOPPLYSNINGER'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTATT_KLAGE' THEN 'KLAGE'
                                  WHEN initielt_vurderingsbehov.aarsak = 'FASTSATT_PERIODE_PASSERT' THEN 'FASTSATT_PERIODE_PASSERT'
                                  WHEN initielt_vurderingsbehov.aarsak = 'FRITAK_MELDEPLIKT' THEN 'FRITAK_MELDEPLIKT'
                                  WHEN initielt_vurderingsbehov.aarsak = 'MOTTATT_KABAL_HENDELSE' THEN 'SVAR_FRA_KLAGEINSTANS'
                                  WHEN initielt_vurderingsbehov.aarsak = 'OPPFØLGINGSOPPGAVE' THEN 'OPPFØLGINGSOPPGAVE'
                                  ELSE 'MANUELL_OPPRETTELSE'
    END)
from (select distinct on (vb.behandling_id) vb.aarsak, vb.behandling_id
      from vurderingsbehov vb
      order by vb.behandling_id, vb.opprettet_tid) as initielt_vurderingsbehov
where behandling.id = initielt_vurderingsbehov.behandling_id
  and aarsak_til_opprettelse is null;
