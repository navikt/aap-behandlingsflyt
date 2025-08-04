ALTER TABLE behandling
    ADD COLUMN aarsak_til_opprettelse VARCHAR(50);
-- Migrere eksisterende behandlinger med årsak senere