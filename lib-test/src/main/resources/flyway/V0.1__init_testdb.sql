/*
Må være med pga. de første linjene i V1.1__Initial_schema_setup.sql:
    CREATE EXTENSION IF NOT EXISTS BTREE_GIST;
    GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO CLOUDSQLIAMUSER;

Det ble lagt til for å gi brukere individuell tilgang til GCP-databasene, men på tidspunktet det ble lagt til var det
strengt tatt ikke nødvendig. Så inntil vi eventuelt baseliner og rydder opp i scriptene, må vi leve med dette.
*/
CREATE SEQUENCE TESTIDENT;

DROP ROLE IF EXISTS cloudsqliamuser;
CREATE ROLE cloudsqliamuser;


-- Starter alle sekvenser på tilfeldige tall slik at ID-er i tester ikke alltid begynner på 1.
-- Dette forhindrer falske positiver i slette-tester der alle rader tilfeldigvis har id=1.
DO
$$
    DECLARE
        seq RECORD;
    BEGIN
        FOR seq IN (SELECT sequence_name, minimum_value::BIGINT AS min_val
                    FROM information_schema.sequences
                    WHERE sequence_schema = 'public')
            LOOP
                EXECUTE format(
                        'ALTER SEQUENCE %I RESTART WITH %s',
                        seq.sequence_name,
                        seq.min_val + floor(random() * 99000)::BIGINT
                        );
            END LOOP;
    END;
$$;

