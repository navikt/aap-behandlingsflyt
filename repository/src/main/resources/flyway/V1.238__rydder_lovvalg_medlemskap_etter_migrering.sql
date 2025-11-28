-- Denne er ikke lenger i bruk nå som vurderingene er periodisert
alter table medlemskap_arbeid_og_inntekt_i_norge_grunnlag drop column manuell_vurdering_id;

alter table lovvalg_medlemskap_manuell_vurdering
    alter column fom set not null,
    alter column vurdert_i_behandling set not null,
    alter column vurderinger_id set not null,
    alter column lovvalgs_land set not null;