ALTER TABLE barnopplysning
    ADD COLUMN navn TEXT;

ALTER TABLE barnopplysning
    alter column ident drop not null;