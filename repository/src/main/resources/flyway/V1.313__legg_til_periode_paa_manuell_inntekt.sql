-- Støtter manuell inntekt per delperiode (før/etter endring i uføregrad) innen ett år.
-- Eksisterende rader har PERIODE_FOM/TOM = NULL og betyr fortsatt «hele året» (bakoverkompatibelt).
ALTER TABLE MANUELL_INNTEKT_VURDERING
    ADD COLUMN PERIODE_FOM DATE,
    ADD COLUMN PERIODE_TOM DATE;

-- fom/tom må settes samtidig, og fom må være <= tom.
ALTER TABLE MANUELL_INNTEKT_VURDERING
    ADD CONSTRAINT CHK_MANUELL_INNTEKT_PERIODE
        CHECK (
            (PERIODE_FOM IS NULL) = (PERIODE_TOM IS NULL)
            AND (PERIODE_FOM IS NULL OR PERIODE_FOM <= PERIODE_TOM)
        );

-- Tidligere: kun én vurdering per år. Nå tillates flere delperioder per år (én rad per
-- uføregrad-segment), men fortsatt unikt per (vurderinger_id, år, periode-fom).
-- NULLS NOT DISTINCT (Postgres 15+) sikrer maks én år-rad (periode = NULL) per år.
DROP INDEX UIDX_MANUELL_INNTEKT_VURDERING;
CREATE UNIQUE INDEX UIDX_MANUELL_INNTEKT_VURDERING
    ON MANUELL_INNTEKT_VURDERING (MANUELL_INNTEKT_VURDERINGER_ID, AR, PERIODE_FOM) NULLS NOT DISTINCT;
