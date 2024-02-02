-- give access to IAM users (GCP)
create extension if not exists btree_gist;
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE PERSON
(
    ID        BIGSERIAL NOT NULL PRIMARY KEY,
    REFERANSE uuid      NOT NULL UNIQUE
);
CREATE INDEX IDX_PERSON_REFERANSE ON PERSON (REFERANSE);

CREATE TABLE PERSON_IDENT
(
    ID        BIGSERIAL          NOT NULL PRIMARY KEY,
    PERSON_ID BIGINT             NOT NULL REFERENCES PERSON (ID),
    IDENT     varchar(19) UNIQUE NOT NULL
);

CREATE INDEX IDX_PERSON_IDENT_IDENT ON PERSON_IDENT (IDENT);

CREATE TABLE SAK
(
    ID                BIGSERIAL                              NOT NULL PRIMARY KEY,
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

CREATE TABLE BEHANDLING
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    sak_id        BIGINT                                 NOT NULL REFERENCES sak (ID),
    referanse     uuid unique                            NOT NULL,
    STATUS        VARCHAR(100)                           NOT NULL,
    type          varchar(100)                           not null,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IDX_BEHANDLING_REFERANSE ON BEHANDLING (referanse);
CREATE INDEX IDX_BEHANDLING_SAK_TID ON BEHANDLING (sak_id, OPPRETTET_TID);

CREATE TABLE AVKLARINGSBEHOV
(
    ID              BIGSERIAL                              NOT NULL PRIMARY KEY,
    behandling_id   BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    definisjon      varchar(50)                            not null,
    funnet_i_steg   varchar(50)                            not null,
    krever_to_trinn boolean,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_AVKLARINGSBEHOV_DEFINISJON ON AVKLARINGSBEHOV (definisjon);
CREATE unique INDEX IDX_AVKLARINGSBEHOV_BEHANDLING_DEFINISJON ON AVKLARINGSBEHOV (behandling_id, definisjon);

CREATE TABLE AVKLARINGSBEHOV_ENDRING
(
    ID                 BIGSERIAL                              NOT NULL PRIMARY KEY,
    avklaringsbehov_id BIGINT                                 NOT NULL REFERENCES AVKLARINGSBEHOV (ID),
    status             varchar(50)                            not null,
    begrunnelse        text,
    OPPRETTET_AV       varchar(100)                           NOT NULL,
    OPPRETTET_TID      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_AVKLARINGSBEHOV_TID ON AVKLARINGSBEHOV (OPPRETTET_TID);

CREATE TABLE STEG_HISTORIKK
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    behandling_id BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    aktiv         boolean      default true              NOT NULL,
    steg          varchar(50)                            not null,
    status        varchar(50)                            not null,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX UIDX_STEG_HISTORIKK ON STEG_HISTORIKK (BEHANDLING_ID) WHERE (AKTIV = TRUE);

--

CREATE TABLE VILKAR_RESULTAT
(
    ID            BIGSERIAL            NOT NULL PRIMARY KEY,
    behandling_id BIGINT               NOT NULL REFERENCES BEHANDLING (ID),
    aktiv         boolean default true NOT NULL
);

CREATE UNIQUE INDEX UIDX_VILKAR_RESULTAT_HISTORIKK ON VILKAR_RESULTAT (BEHANDLING_ID) WHERE (AKTIV = TRUE);

CREATE TABLE VILKAR
(
    ID          BIGSERIAL   NOT NULL PRIMARY KEY,
    type        varchar(50) NOT NULL,
    resultat_id bigint      not null references VILKAR_RESULTAT (ID)
);

CREATE UNIQUE INDEX UIDX_VILKAR ON VILKAR (resultat_id, type);

CREATE TABLE VILKAR_PERIODE
(
    ID                BIGSERIAL     NOT NULL PRIMARY KEY,
    vilkar_id         bigint        not null references VILKAR (ID),
    periode           daterange     NOT NULL,
    utfall            varchar(50)   NOT NULL,
    manuell_vurdering boolean       not null,
    begrunnelse       text          null,
    innvilgelsesarsak varchar(100)  null,
    avslagsarsak      varchar(100)  null,
    faktagrunnlag     varchar(4000) null,
    versjon           varchar(100)  not null
);
CREATE INDEX IDX_VILKAR_PERIODE_PERIODE ON VILKAR_PERIODE (vilkar_id, periode);
alter table VILKAR_PERIODE
    add constraint VILKAR_PERIODE_ikke_overlapp_periode EXCLUDE USING GIST (
        vilkar_id WITH =,
        periode WITH &&
        );

--
CREATE TABLE OPPGAVE
(
    id            BIGSERIAL                              NOT NULL PRIMARY KEY,
    status        varchar(50)  DEFAULT 'KLAR'            not null,
    type          varchar(50)                            not null,
    sak_id        bigint                                 null REFERENCES SAK (id),
    behandling_id bigint                                 null REFERENCES BEHANDLING (id),
    neste_kjoring TIMESTAMP(3)                           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_OPPGAVE_STATUS ON OPPGAVE (status, sak_id, behandling_id, neste_kjoring);
CREATE TABLE OPPGAVE_HISTORIKK
(
    id            BIGSERIAL                              NOT NULL PRIMARY KEY,
    oppgave_id    BIGINT                                 not null REFERENCES OPPGAVE (id),
    status        varchar(50)                            not null,
    feilmelding   text                                   null,
    opprettet_tid TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IDX_OPPGAVE_HISTORIKK_STATUS ON OPPGAVE_HISTORIKK (oppgave_id, status);

-- yrkesskadegrunnlag

CREATE TABLE YRKESSKADE
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE YRKESSKADE_PERIODER
(
    ID            BIGSERIAL NOT NULL PRIMARY KEY,
    YRKESSKADE_ID BIGINT    NOT NULL REFERENCES YRKESSKADE (ID),
    REFERANSE     TEXT      NOT NULL,
    PERIODE       DATERANGE NOT NULL
);

CREATE INDEX IDX_YRKESSKADE_PERIODER_GRUNNLAG_ID ON YRKESSKADE_PERIODER (YRKESSKADE_ID);

CREATE TABLE YRKESSKADE_GRUNNLAG
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    YRKESSKADE_ID BIGINT                                 NOT NULL REFERENCES YRKESSKADE (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_YRKESSKADE_GRUNNLAG_BEHANDLING_ID ON YRKESSKADE_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);

-- UFØRE GRUNNLAG

CREATE TABLE UFORE
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    UFOREGRAD     SMALLINT                               NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE UFORE_GRUNNLAG
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    UFORE_ID      BIGINT                                 NOT NULL REFERENCES UFORE (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_UFORE_GRUNNLAG_BEHANDLING_ID ON UFORE_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);

-- Underveis grunnlag
CREATE TABLE UNDERVEIS_PERIODER
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE UNDERVEIS_PERIODE
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    perioder_id   BIGINT                                 NOT NULL REFERENCES UNDERVEIS_PERIODER (ID),
    PERIODE       daterange                              NOT NULL,
    timer_arbeid  NUMERIC(5, 1)                          NULL,
    utfall        varchar(50)                            NOT NULL,
    avslagsarsak  varchar(50)                            NULL,
    grenseverdi   SMALLINT                               NOT NULL,
    gradering     SMALLINT                               NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
alter table UNDERVEIS_PERIODE
    add constraint UNDERVEIS_PERIODE_IKKE_OVERLAPP_PERIODE EXCLUDE USING GIST (
        perioder_id WITH =,
        PERIODE WITH &&
        );

CREATE TABLE UNDERVEIS_GRUNNLAG
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    PERIODER_ID   BIGINT                                 NOT NULL REFERENCES UNDERVEIS_PERIODER (ID),
    AKTIV         BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX UIDX_UNDERVEIS_GRUNNLAG_BEHANDLING_ID ON UNDERVEIS_GRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);
