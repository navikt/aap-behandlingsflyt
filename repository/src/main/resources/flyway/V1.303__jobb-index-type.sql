-- Inkluderer TYPE-kolonnen i indeksen slik at ekskluderende_jobb-CTE-en i Motor sin plukkJobb-spørring
-- kan bruke indeksen for DISTINCT ON (sak_id, behandling_id, type) ORDER BY sak_id, behandling_id, type, neste_kjoring
-- uten å måtte sortere i minnet.
DROP INDEX IDX_OPPGAVE_STATUS;
CREATE INDEX IDX_JOBB_STATUS_SAK_BEHANDLING_TYPE ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, TYPE, NESTE_KJORING);
