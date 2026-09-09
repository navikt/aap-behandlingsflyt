ALTER TABLE krav_vurdering
    ADD COLUMN arena_saksnummer TEXT,
    ADD COLUMN rettighetstype TEXT,
    ADD COLUMN resterende_kvote_ordinaer INT,
    ADD COLUMN virkningstidspunkt_arena DATE NULL,
    ALTER COLUMN journalpost_id DROP NOT NULL;
