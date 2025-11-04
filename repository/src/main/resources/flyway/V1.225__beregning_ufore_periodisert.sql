create table beregning_ufore_tidsperiode(
    ID            BIGSERIAL PRIMARY KEY,
    BEREGNING_UFORE_INNTEKT_ID BIGINT REFERENCES BEREGNING_UFORE_INNTEKT,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PERIODE       daterange,
    INNTEKT_I_KRONER numeric(19,2),
    INNTEKT_I_G numeric(21,10),
    INNTEKT_JUSTERT_FOR_UFOREGRAD numeric(19,2),
    inntekt_justert_ufore_g numeric(21,10),
    GRUNNBELOP numeric(19,2),
    UFOREGRAD smallint,
    ARBEIDSGRAD smallint
)