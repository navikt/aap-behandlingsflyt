drop index idx_beregningsgrunnlag_aktiv_behandling_id_beregning_id;
create index idx_beregningsgrunnlag_aktiv_behandling_id
    on beregningsgrunnlag (behandling_id)
    where (aktiv = true);