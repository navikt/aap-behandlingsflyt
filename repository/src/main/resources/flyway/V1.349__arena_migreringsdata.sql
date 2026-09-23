CREATE TABLE arena_migreringsdata
(
    id               BIGSERIAL    NOT NULL PRIMARY KEY,
    behandling_id    BIGINT       NOT NULL REFERENCES behandling (id),
    steg             VARCHAR(100) NOT NULL,
    aktiv            BOOLEAN      NOT NULL DEFAULT TRUE,
    hentet_tidspunkt TIMESTAMP(3) NOT NULL,
    data             JSONB        NOT NULL
);

-- Maks én aktiv rad per behandling og steg. Dekker også oppslag på aktive rader.
CREATE UNIQUE INDEX uidx_arena_migreringsdata_behandling_steg_aktiv
    ON arena_migreringsdata (behandling_id, steg)
    WHERE aktiv = TRUE;
