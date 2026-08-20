ALTER TABLE avslag_11_27_vurdering
DROP COLUMN sykepengegrunnlag,
    ADD COLUMN har_sykepengegrunnlag_over_2g BOOLEAN;