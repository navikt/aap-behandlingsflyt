-- Fjerner DEFAULT CURRENT_TIMESTAMP fra rettighetsperiode-tabeller.
ALTER TABLE rettighetsperiode_vurderinger
    ALTER COLUMN opprettet DROP DEFAULT;
ALTER TABLE rettighetsperiode_vurdering
    ALTER COLUMN opprettet DROP DEFAULT;
ALTER TABLE rettighetsperiode_grunnlag
    ALTER COLUMN opprettet DROP DEFAULT;
