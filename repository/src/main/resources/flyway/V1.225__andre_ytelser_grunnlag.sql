CREATE TABLE ANDRE_YTELSER_SVAR_I_SØKNAD
(
    id            BIGSERIAL PRIMARY KEY,
    opprettet_tid TIMESTAMP(3) DEFAULT current_timestamp NOT NULL,
    ekstraLønn    BOOLEAN                                NOT NULL,
    afpKilder     TEXT
);

CREATE TABLE ANDRE_YTELSE_OPPGITT_I_SØKNAD
(
    id               BIGSERIAL PRIMARY KEY,
    andre_ytelser_id BIGINT NOT NULL REFERENCES ANDRE_YTELSER_SVAR_I_SØKNAD (id),
    ytelse           TEXT   NOT NULL
);

CREATE TABLE ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG
(
    id               BIGSERIAL PRIMARY KEY,
    behandling_id    BIGINT               NOT NULL REFERENCES behandling (id),
    andre_ytelser_id BIGINT REFERENCES ANDRE_YTELSER_SVAR_I_SØKNAD (id),
    aktiv            BOOLEAN DEFAULT TRUE NOT NULL
);