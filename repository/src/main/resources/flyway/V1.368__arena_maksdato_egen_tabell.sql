CREATE TABLE arena_maksdato
(
    id               BIGSERIAL    NOT NULL PRIMARY KEY,
    arena_saksnummer TEXT         NOT NULL,
    status           TEXT         NOT NULL,
    maksdato         DATE,
    vedtak_til       DATE,
    vedtak_id        BIGINT       NOT NULL,
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE arena_maksdato_grunnlag
    ADD COLUMN arena_maksdato_id BIGINT NULL REFERENCES arena_maksdato (id);

CREATE INDEX idx_arena_maksdato_grunnlag_arena_maksdato_id
    ON arena_maksdato_grunnlag (arena_maksdato_id);

-- Funksjonen er ikke i bruk i prod (tabellen er tom der). Flytter eventuelle data i dev.
INSERT INTO arena_maksdato (id, arena_saksnummer, status, maksdato, vedtak_til, vedtak_id, opprettet_tid)
SELECT id, arena_saksnummer, status, maksdato, vedtak_til, vedtak_id, opprettet_tid
FROM arena_maksdato_grunnlag
WHERE arena_saksnummer IS NOT NULL;

UPDATE arena_maksdato_grunnlag
SET arena_maksdato_id = id
WHERE arena_saksnummer IS NOT NULL;

SELECT setval(pg_get_serial_sequence('arena_maksdato', 'id'), COALESCE(MAX(id), 0) + 1, false)
FROM arena_maksdato;

ALTER TABLE arena_maksdato_grunnlag
    DROP COLUMN arena_saksnummer,
    DROP COLUMN status,
    DROP COLUMN maksdato,
    DROP COLUMN vedtak_til,
    DROP COLUMN vedtak_id;
