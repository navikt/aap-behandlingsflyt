ALTER TABLE barnopplysning
    ADD COLUMN fodselsdato DATE,
    ADD COLUMN dodsdato    DATE,
    ADD COLUMN person_id   INT references person (id);

UPDATE barnopplysning
SET fodselsdato = po.fodselsdato,
    dodsdato    = po.dodsdato,
    person_id   = p.id
FROM person p
         join personopplysning po on p.id = po.person_id
WHERE barnopplysning.ident in (select ident from person_ident where person_id = p.id);

ALTER TABLE barnopplysning
    ALTER COLUMN ident drop not null;

ALTER TABLE OPPGITT_BARN
    ALTER COLUMN ident drop not null;

ALTER TABLE barn_tillegg
    ADD COLUMN navn        TEXT,
    ADD COLUMN fodselsdato DATE,
    ADD COLUMN person_id   INT references person (id);

ALTER TABLE barn_tillegg
    ALTER COLUMN ident drop not null;

ALTER TABLE barn_vurdering
    ADD COLUMN navn        TEXT,
    ADD COLUMN fodselsdato DATE,
    ADD COLUMN person_id   INT references person (id);

ALTER TABLE barn_vurdering
    ALTER COLUMN ident drop not null;