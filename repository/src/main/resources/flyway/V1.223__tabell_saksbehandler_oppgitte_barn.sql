-- Barn oppgitt av saksbehandler i Kelvin

CREATE TABLE BARN_SAKSBEHANDLER_OPPGITT_BARNOPPLYSNING
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE BARNOPPLYSNING_GRUNNLAG
    ADD COLUMN saksbehandler_oppgitt_barn_id BIGINT references BARN_SAKSBEHANDLER_OPPGITT_BARNOPPLYSNING;

CREATE TABLE BARN_SAKSBEHANDLER_OPPGITT
(
    ID                            BIGSERIAL                              NOT NULL PRIMARY KEY,
    IDENT                         TEXT,
    NAVN                          TEXT                                   NOT NULL,
    FODSELSDATO                   DATE                                   NOT NULL,
    RELASJON                      TEXT                                   NOT NULL,
    OPPRETTET_TID                 TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    saksbehandler_oppgitt_barn_id BIGINT                                 NOT NULL REFERENCES BARN_SAKSBEHANDLER_OPPGITT_BARNOPPLYSNING (ID)
);

CREATE UNIQUE INDEX UIDX_SAKSBEHANDLER_OPPGITT_BARN_IDENT ON BARN_SAKSBEHANDLER_OPPGITT (saksbehandler_oppgitt_barn_id, IDENT);
