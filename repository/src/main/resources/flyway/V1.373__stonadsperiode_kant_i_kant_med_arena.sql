ALTER TABLE stonadsperiode_vurdering
    ADD COLUMN kant_i_kant_utfall            TEXT   NULL,
    ADD COLUMN kant_i_kant_arena_maksdato_id BIGINT NULL REFERENCES arena_maksdato (id);

CREATE INDEX idx_stonadsperiode_vurdering_kant_i_kant_arena_maksdato_id
    ON stonadsperiode_vurdering (kant_i_kant_arena_maksdato_id);
