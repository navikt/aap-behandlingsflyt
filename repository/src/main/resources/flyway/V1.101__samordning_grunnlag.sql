CREATE TABLE samordning_ytelse_grunnlag
(
    ID                   BIGSERIAL PRIMARY KEY,
    BEHANDLING_ID        BIGINT  NOT NULL REFERENCES BEHANDLING (ID),
    OPPRETTET            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    AKTIV                BOOLEAN NOT NULL,
    samordning_ytelse_id bigint  not null references samordning_ytelser (id)
);

CREATE UNIQUE INDEX UIDX_SAMORDING_YTELSE_GRUNNLAG_BEHANDLING_ID ON samordning_ytelse_grunnlag (BEHANDLING_ID) WHERE (AKTIV = TRUE);

ALTER TABLE samordning_ytelsevurdering_grunnlag
    alter column ytelser_id drop not null;

-- todo: drop table samordning_ytelser