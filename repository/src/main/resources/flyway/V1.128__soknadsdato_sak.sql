ALTER TABLE sak
    ADD COLUMN soknadstidspunkt DATE;

UPDATE sak
SET soknadstidspunkt = lower(sak2.rettighetsperiode)
FROM sak sak2
WHERE sak2.id = sak.id;

ALTER TABLE sak
    ALTER COLUMN soknadstidspunkt
        SET NOT NULL;