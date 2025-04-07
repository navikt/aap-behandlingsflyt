ALTER TABLE meldekort
    ADD COLUMN mottatt_tidspunkt TIMESTAMP(3);

UPDATE meldekort
SET mottatt_tidspunkt = md.mottatt_tid
FROM mottatt_dokument md
WHERE meldekort.journalpost = md.referanse
  AND md.type = 'MELDEKORT';

ALTER TABLE meldekort
    ALTER COLUMN mottatt_tidspunkt SET NOT NULL;
