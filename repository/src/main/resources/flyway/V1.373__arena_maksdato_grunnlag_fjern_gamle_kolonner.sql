ALTER TABLE arena_maksdato_grunnlag
    DROP COLUMN arena_saksnummer,
    DROP COLUMN status,
    DROP COLUMN maksdato,
    DROP COLUMN vedtak_til,
    DROP COLUMN vedtak_id;

ALTER TABLE arena_maksdato
    DROP COLUMN status;
