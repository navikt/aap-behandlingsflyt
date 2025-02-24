ALTER TABLE underveis_periode
    ADD COLUMN institusjonsoppholdreduksjon INT NOT NULL DEFAULT 0;

ALTER TABLE UNDERVEIS_PERIODE
    ALTER COLUMN institusjonsoppholdreduksjon DROP DEFAULT;