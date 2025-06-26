ALTER TABLE avvist_formkrav_varsel ADD COLUMN behandling_id bigint;

UPDATE avvist_formkrav_varsel afv
SET behandling_id = eafg.behandling_id
FROM effektuer_avvist_formkrav_grunnlag eafg
WHERE afv.id = eafg.varsel_id;

ALTER TABLE avvist_formkrav_varsel ALTER COLUMN behandling_id SET NOT NULL;
ALTER TABLE avvist_formkrav_varsel ADD CONSTRAINT fk_avvist_formkrav_varsel_behandling FOREIGN KEY (behandling_id) REFERENCES behandling (id);
ALTER TABLE avvist_formkrav_varsel ADD CONSTRAINT uq_avvist_formkrav_varsel_behandling_id UNIQUE (behandling_id);

DROP INDEX IF EXISTS idx_effektuer_avvist_formkrav_grunnlag_behandling_id;
DROP TABLE IF EXISTS effektuer_avvist_formkrav_grunnlag;
DROP TABLE IF EXISTS effektuer_avvist_formkrav_vurdering;
