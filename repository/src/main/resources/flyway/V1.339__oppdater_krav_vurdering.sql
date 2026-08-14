ALTER TABLE krav_vurdering
    ALTER COLUMN journalpost_id DROP NOT NULL;

ALTER TABLE krav_vurdering
    ADD COLUMN saksnummer_arena TEXT NULL,
    ADD COLUMN migreringsdato   DATE NULL,
    ADD COLUMN resterende_kvote INT  NULL;
