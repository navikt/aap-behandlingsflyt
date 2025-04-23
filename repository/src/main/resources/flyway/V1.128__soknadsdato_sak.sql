ALTER TABLE sak
    ADD COLUMN soknadsdato DATE;

UPDATE sak
SET soknadsdato = lower(sak2.rettighetsperiode)
FROM sak sak2
WHERE sak2.id = sak.id;

ALTER TABLE sak
    ALTER COLUMN soknadsdato
        SET NOT NULL;