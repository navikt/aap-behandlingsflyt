CREATE TABLE YTELSE
(
    type TEXT PRIMARY KEY
);

CREATE TABLE ANDRE_YTELSER_GRUNNLAG
(
    id            BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT                                 NOT NULL REFERENCES behandling (id),
    lonn          BOOLEAN                                NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT current_timestamp NOT NULL
);

CREATE TABLE YTELSER
(
    ytelse_grunnlag BIGINT NOT NULL REFERENCES ANDRE_YTELSER_GRUNNLAG(id),
    ytelse_type     TEXT   NOT NULL REFERENCES YTELSE(type),
    PRIMARY KEY (ytelse_grunnlag, ytelse_type)
);
