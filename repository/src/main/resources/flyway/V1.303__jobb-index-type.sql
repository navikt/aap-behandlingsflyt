-- Inkluderer TYPE-kolonnen i indeksen slik at ekskluderende_jobb-CTE-en i Motor sin plukkJobb-spørring
-- kan bruke indeksen for DISTINCT ON (sak_id, behandling_id, type) ORDER BY sak_id, behandling_id, type, neste_kjoring
-- uten å måtte sortere i minnet.
--DROP INDEX IDX_OPPGAVE_STATUS;

-- obs! legg inn denne indexen manuelt i prod db for å unngå write-lock på jobb-tabellen
-- manuelt: CREATE INDEX CONCURRENTLY IF NOT EXISTS IDX_JOBB_STATUS_SAK_BEHANDLING_TYPE ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, TYPE, NESTE_KJORING);
-- flyway-dummy for referanse? (uten concurrently (ikke støtta av flyway) vil denne medføre write-lock hvis ikke manuelt lagt inn først med concurrently):
-- CREATE INDEX IF NOT EXISTS IDX_JOBB_STATUS_SAK_BEHANDLING_TYPE ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, TYPE, NESTE_KJORING);
