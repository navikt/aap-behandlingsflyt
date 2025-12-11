ALTER TABLE trukket_soknad_vurdering
    ADD COLUMN skal_trekkes boolean NOT NULL DEFAULT true;

ALTER TABLE trukket_soknad_vurdering
    ALTER COLUMN skal_trekkes DROP DEFAULT;
