ALTER TABLE UNDERVEIS_PERIODE
    ALTER COLUMN timer_arbeid TYPE numeric(5, 1) USING (COALESCE(timer_arbeid, 0)),
    ALTER COLUMN timer_arbeid SET DEFAULT 0,
    ALTER COLUMN timer_arbeid SET NOT NULL,

    ALTER COLUMN gradering TYPE smallint USING (COALESCE(underveis_periode.gradering, 0)),
    ALTER COLUMN gradering SET DEFAULT 0,
    ALTER COLUMN gradering SET NOT NULL,

    ALTER COLUMN andel_arbeidsevne TYPE smallint USING (COALESCE(andel_arbeidsevne, 0)),
    ALTER COLUMN andel_arbeidsevne SET DEFAULT 0,
    ALTER COLUMN andel_arbeidsevne SET NOT NULL;