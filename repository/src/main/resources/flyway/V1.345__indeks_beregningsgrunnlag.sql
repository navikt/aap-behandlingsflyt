create index idx_beregningsgrunnlag_aktiv_behandling_id_beregning_id
    on beregningsgrunnlag (behandling_id, beregning_id)
    where (aktiv = true);