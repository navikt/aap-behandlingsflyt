DROP INDEX IF EXISTS idx_effektuer_avvist_formkrav_grunnlag_behandling_id;
DROP TABLE IF EXISTS effektuer_avvist_formkrav_grunnlag;
DROP TABLE IF EXISTS effektuer_avvist_formkrav_vurdering;

DELETE FROM avvist_formkrav_varsel;
TRUNCATE TABLE avvist_formkrav_varsel;

ALTER TABLE avvist_formkrav_varsel ADD COLUMN behandling_id bigint NOT NULL;
ALTER TABLE avvist_formkrav_varsel ADD CONSTRAINT fk_avvist_formkrav_varsel_behandling FOREIGN KEY (behandling_id) REFERENCES behandling (id);
ALTER TABLE avvist_formkrav_varsel ADD CONSTRAINT uq_avvist_formkrav_varsel_behandling_id UNIQUE (behandling_id);

