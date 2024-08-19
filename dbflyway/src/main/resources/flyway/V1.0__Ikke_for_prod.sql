-- Denne fila skal slettes før opprettelse av prod-db
-- Riktig sekvensnummer settes i V1.1__Initial_schema_setup.sql

-- Bump dette sekvensnummeret med 10000 før resetting av db i dev
CREATE SEQUENCE IF NOT EXISTS SEQ_SAKSNUMMER INCREMENT BY 50 MINVALUE 10010000;
