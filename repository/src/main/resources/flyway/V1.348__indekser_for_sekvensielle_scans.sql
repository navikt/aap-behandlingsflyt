-- Indekser for spørringer som gjorde sekvensielle scans i produksjon.
create index idx_medlemskap_arbeid_inntekt_norge_grunnlag_arbeider_id
    on medlemskap_arbeid_og_inntekt_i_norge_grunnlag (arbeider_id);

create index idx_medlemskap_arbeid_inntekt_norge_grunnlag_inntekter_id
    on medlemskap_arbeid_og_inntekt_i_norge_grunnlag (inntekter_i_norge_id);

create index idx_vilkar_resultat_behandling_id
    on vilkar_resultat (behandling_id);

create index idx_samordning_ytelse_grunnlag_behandling_id
    on samordning_ytelse_grunnlag (behandling_id);

create index idx_sykdom_vurdering_brev_behandling_id_opprettet_tid
    on sykdom_vurdering_brev (behandling_id, opprettet_tid desc);

create index idx_stans_opphor_stans_opphor_set_id
    on stans_opphor (stans_opphor_set_id);

create index idx_beregning_hoved_beregning_id
    on beregning_hoved (beregning_id);

create index idx_barn_tillegg_barnetillegg_periode_id
    on barn_tillegg (barnetillegg_periode_id);

create index idx_rettighetstype_periode_perioder_id
    on rettighetstype_periode (perioder_id);

create index idx_inntekt_periode_inntekt_id
    on inntekt_periode (inntekt_id);

create index idx_samordning_ytelse_periode_ytelse_id
    on samordning_ytelse_periode (ytelse_id);
