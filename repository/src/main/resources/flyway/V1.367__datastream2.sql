DO
$$
    BEGIN
        IF EXISTS(SELECT * FROM pg_roles WHERE rolname = 'datastream') THEN
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$ LANGUAGE 'plpgsql';
