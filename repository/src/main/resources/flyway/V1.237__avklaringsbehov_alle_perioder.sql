alter table avklaringsbehov_endring
add column perioder_ugyldig_vurdering daterange[];
alter table avklaringsbehov_endring
add column perioder_krever_vurdering daterange[];