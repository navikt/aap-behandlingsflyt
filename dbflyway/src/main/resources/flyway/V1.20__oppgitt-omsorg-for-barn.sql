-- oppgitte barn

ALTER TABLE BARNOPPLYSNING_GRUNNLAG
    rename column bgb_id to register_barn_id;
ALTER TABLE barnopplysning_grunnlag ALTER COLUMN register_barn_id DROP NOT NULL;

CREATE TABLE OPPGITT_BARNOPPLYSNING
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE BARNOPPLYSNING_GRUNNLAG
    ADD COLUMN oppgitt_barn_id BIGINT references OPPGITT_BARNOPPLYSNING;

CREATE TABLE OPPGITT_BARN
(
    ID              BIGSERIAL                              NOT NULL PRIMARY KEY,
    IDENT           VARCHAR(11)                            NOT NULL,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    oppgitt_barn_id BIGINT                                 NOT NULL REFERENCES OPPGITT_BARNOPPLYSNING (ID)
);
CREATE UNIQUE INDEX UIDX_OPPGITT_BARN_IDENT ON OPPGITT_BARN (oppgitt_barn_id, IDENT);

-- Flytter personopplysninger så vi skiller registerdata og fakta om relasjoner

ALTER TABLE BARNOPPLYSNING
    DROP column fodselsdato;
ALTER TABLE BARNOPPLYSNING
    DROP column dodsdato;

CREATE TABLE PERSONOPPLYSNINGER
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE PERSONOPPLYSNING
    RENAME TO BRUKER_PERSONOPPLYSNING;
ALTER TABLE PERSONOPPLYSNING_GRUNNLAG
    RENAME COLUMN personopplysning_id to bruker_personopplysning_id;

CREATE TABLE PERSONOPPLYSNING
(
    ID                    BIGSERIAL                              NOT NULL PRIMARY KEY,
    person_id             bigint                                 not null REFERENCES PERSON (ID),
    FODSELSDATO           DATE                                   NOT NULL,
    DODSDATO              DATE         DEFAULT NULL,
    personopplysninger_id BIGINT                                 NOT NULL references PERSONOPPLYSNINGER (ID),
    OPPRETTET_TID         TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX UIDX_PERSONOPPLYSNING ON PERSONOPPLYSNING (person_id, personopplysninger_id);

ALTER TABLE PERSONOPPLYSNING_GRUNNLAG
    ADD COLUMN personopplysninger_id BIGINT references PERSONOPPLYSNINGER (ID);
