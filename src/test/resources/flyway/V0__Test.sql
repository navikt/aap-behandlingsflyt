-- V0__Test.sql brukes til å opprette ting som finnes på GCP.
DROP ROLE IF EXISTS cloudsqliamuser;
CREATE ROLE cloudsqliamuser;

CREATE TABLE TEST
(
    ID   SERIAL NOT NULL PRIMARY KEY,
    TEST TEXT
);
