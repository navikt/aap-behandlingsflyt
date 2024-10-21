ALTER TABLE BRUDD_AKTIVITETSPLIKT
    DROP COLUMN ERSTATTER,
    ADD COLUMN DOKUMENT_TYPE TEXT NOT NULL DEFAULT 'BRUDD';

-- Fjern default etter at endring er rullet ut og backfill utført.