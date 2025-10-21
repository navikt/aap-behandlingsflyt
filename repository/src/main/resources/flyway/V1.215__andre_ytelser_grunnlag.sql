CREATE TABLE YTELSE
(
    type TEXT PRIMARY KEY
);

CREATE TABLE ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG
(
    id            BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT                                 NOT NULL REFERENCES behandling (id),
    lonn          BOOLEAN                                NOT NULL,
    aktiv         BOOLEAN      DEFAULT TRUE              NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT current_timestamp NOT NULL
);

CREATE TABLE ANDRE_YTELSER_OPPGITT_I_SØKNAD
(
    ytelse_grunnlag BIGINT NOT NULL REFERENCES ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG (id),
    ytelse_type     TEXT   NOT NULL REFERENCES YTELSE (type),
    PRIMARY KEY (ytelse_grunnlag, ytelse_type)
);
