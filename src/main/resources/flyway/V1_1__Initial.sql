-- give access to IAM users (GCP)
create extension if not exists btree_gist;
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE test
(
    id SERIAL PRIMARY KEY,
    test TEXT
);

CREATE TABLE PERSON
(
    ID        SERIAL NOT NULL PRIMARY KEY,
    REFERANSE uuid   NOT NULL UNIQUE
);
CREATE INDEX IDX_PERSON_REFERANSE ON PERSON (REFERANSE);

CREATE TABLE PERSON_IDENT
(
    ID        SERIAL             NOT NULL PRIMARY KEY,
    PERSON_ID BIGINT             NOT NULL REFERENCES PERSON (ID),
    IDENT     varchar(19) UNIQUE NOT NULL
);

CREATE INDEX IDX_PERSON_IDENT_IDENT ON PERSON_IDENT (IDENT);

CREATE TABLE SAK
(
    ID                SERIAL                                 NOT NULL PRIMARY KEY,
    SAKSNUMMER        VARCHAR(19)                            NOT NULL,
    PERSON_ID         BIGINT                                 NOT NULL REFERENCES PERSON (ID),
    RETTIGHETSPERIODE daterange                              NOT NULL,
    STATUS            VARCHAR(100)                           NOT NULL,
    VERSJON           BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_TID     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- avhenger av: "create extension if not exists btree_gist;" som superbruker

alter table sak
    add constraint sak_ikke_overlapp_periode EXCLUDE USING GIST (
        PERSON_ID WITH =,
        RETTIGHETSPERIODE WITH &&
        );

CREATE INDEX IDX_SAK_SAKSNUMMER ON SAK (SAKSNUMMER);
CREATE INDEX IDX_SAK_PERSON ON SAK (PERSON_ID);

create sequence if not exists SEQ_SAKSNUMMER increment by 50 minvalue 10000000;