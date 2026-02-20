-- Krev at for samme grunnlag kan ikke meldeperioder overlappe hverandre
-- Ideelt sett skulle vi krevd mer, gjennom å si at kombinasjonen
-- meldeperiode.periode og meldeperiode_grunnlag.behandlingId må være unik,
-- men Postgres lar oss ikke gjøre det med en constraint (trigger går an).
ALTER TABLE MELDEPERIODE
    ADD CONSTRAINT MELDEPERIODE_IKKE_OVERLAPPENDE_PERIODE
        EXCLUDE USING GIST (MELDEPERIODEGRUNNLAG_ID WITH =, PERIODE WITH &&);

ALTER TABLE MELDEPERIODE_GRUNNLAG
    ALTER COLUMN OPPRETTET SET NOT NULL;

ALTER TABLE MELDEPERIODE
    ALTER COLUMN OPPRETTET SET NOT NULL;
