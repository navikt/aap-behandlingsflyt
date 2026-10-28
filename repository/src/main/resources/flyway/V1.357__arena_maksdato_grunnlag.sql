CREATE TABLE arena_maksdato_grunnlag
(
    id               BIGSERIAL    NOT NULL PRIMARY KEY,
    behandling_id    BIGINT       NOT NULL REFERENCES behandling (id),
    aktiv            BOOLEAN      NOT NULL DEFAULT TRUE,
    arena_saksnummer TEXT,
    status           TEXT,
    maksdato         DATE,
    vedtak_til       DATE,
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_arena_maksdato_grunnlag_behandling_id
    ON arena_maksdato_grunnlag (behandling_id);

CREATE UNIQUE INDEX idx_arena_maksdato_grunnlag_behandling_aktiv
    ON arena_maksdato_grunnlag (behandling_id)
    WHERE aktiv;
