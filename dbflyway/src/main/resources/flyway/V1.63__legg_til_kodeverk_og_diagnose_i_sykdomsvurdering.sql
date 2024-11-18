-- legger til rad for kodeverk og diagnoser i sykdomsvurdering
ALTER TABLE SYKDOM_VURDERING
    ADD COLUMN kodeverk TEXT DEFAULT NULL,
    ADD COLUMN diagnose TEXT DEFAULT NULL;
