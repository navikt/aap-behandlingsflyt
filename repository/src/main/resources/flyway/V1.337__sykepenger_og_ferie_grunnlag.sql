CREATE TABLE SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD
(
    id                BIGSERIAL PRIMARY KEY,
    opprettet_tid     TIMESTAMP(3) DEFAULT current_timestamp NOT NULL,
    mottar_sykepenger BOOLEAN                                NOT NULL,
    ferie_dager       INT
);

CREATE TABLE SYKEPENGE_FERIEPERIODE_I_SØKNAD
(
    id                    BIGSERIAL PRIMARY KEY,
    sykepenger_ferie_id   BIGINT NOT NULL REFERENCES SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD (id),
    fra_dato              DATE   NOT NULL,
    til_dato              DATE   NOT NULL
);

CREATE TABLE SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG
(
    id                  BIGSERIAL PRIMARY KEY,
    behandling_id       BIGINT               NOT NULL REFERENCES behandling (id),
    sykepenger_ferie_id BIGINT REFERENCES SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD (id),
    aktiv               BOOLEAN DEFAULT TRUE NOT NULL
);
