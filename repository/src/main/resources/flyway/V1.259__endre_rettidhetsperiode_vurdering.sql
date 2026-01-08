ALTER TABLE rettighetsperiode_vurdering
ALTER COLUMN har_rett_utover_soknadsdato TYPE text
    USING CASE
              WHEN har_rett_utover_soknadsdato IS TRUE THEN 'Ja'
              ELSE 'Nei'
    END;