ALTER TABLE avslag_11_27_vurdering
DROP COLUMN har_sykepengegrunnlag_over_2g,
    ADD COLUMN brukers_ytelse_tom DATE,
    ADD COLUMN sykepengegrunnlag NUMERIC,
    ADD COLUMN har_arbeidsgiver_sykepenger_utbetaling BOOLEAN;