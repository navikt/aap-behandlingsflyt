CREATE SEQUENCE TESTIDENT;

DROP ROLE IF EXISTS cloudsqliamuser;
CREATE ROLE cloudsqliamuser;


----

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
