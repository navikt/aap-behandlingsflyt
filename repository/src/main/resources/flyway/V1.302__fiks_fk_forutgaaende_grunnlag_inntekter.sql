-- Fikser feil FK på FORUTGAAENDE_GRUNNLAG.INNTEKTER_I_NORGE_ID.
-- Kolonnen pekte på INNTEKT_I_NORGE_FORUTGAAENDE (enkeltrad) men skal peke på
-- INNTEKTER_I_NORGE_FORUTGAAENDE (aggregerings-tabell) som koden faktisk lagrer ID-en fra.
ALTER TABLE FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
    DROP CONSTRAINT forutgaaende_medlemskap_arbeid_og_inn_inntekter_i_norge_id_fkey;

ALTER TABLE FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
    ADD CONSTRAINT forutgaaende_grunnlag_inntekter_i_norge_id_fkey
        FOREIGN KEY (INNTEKTER_I_NORGE_ID) REFERENCES INNTEKTER_I_NORGE_FORUTGAAENDE (ID);
