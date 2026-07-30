CREATE TABLE test_automatisk_meldekort_sak
(
    sak_id BIGINT NOT NULL PRIMARY KEY REFERENCES sak (id)
);
