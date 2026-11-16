ALTER TABLE stonadsperiode_vurdering
    ADD COLUMN kant_i_kant_med_arena_grunnlag_id BIGINT
        REFERENCES arena_maksdato_grunnlag (id) ON DELETE SET NULL;

CREATE INDEX idx_stonadsperiode_vurdering_kant_i_kant_med_arena_grunnlag_id
    ON stonadsperiode_vurdering (kant_i_kant_med_arena_grunnlag_id);

-- Eksisterende kant i kant-vurderinger peker til aktivt Arena-maksdatogrunnlag
-- i behandlingen der vurderingen ble gjort.
UPDATE stonadsperiode_vurdering v
SET kant_i_kant_med_arena_grunnlag_id = a.id
FROM arena_maksdato_grunnlag a
WHERE v.kant_i_kant_med_arena
  AND a.behandling_id = v.vurdert_i_behandling
  AND a.aktiv;
