ALTER TABLE arena_maksdato
    ADD COLUMN vedtak_status               TEXT,
    ADD COLUMN har_forlengelse_etter_11_12 BOOLEAN;

-- Ny kode skriver ikke lenger status. Kolonnen fjernes i en senere release.
ALTER TABLE arena_maksdato
    ALTER COLUMN status DROP NOT NULL;

COMMENT ON COLUMN arena_maksdato.status IS 'Deprecated: sakstatus fra Arena, erstattet av vedtak_status. Fjernes.';
