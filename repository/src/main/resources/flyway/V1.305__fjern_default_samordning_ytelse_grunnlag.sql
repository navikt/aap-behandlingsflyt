-- Fjerner DEFAULT CURRENT_TIMESTAMP fra samordning_ytelse_grunnlag.
ALTER TABLE samordning_ytelse_grunnlag
    ALTER COLUMN opprettet TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN opprettet DROP DEFAULT;
