-- Tar i bruk innstillingene fra beforeMigrate__1_testdb_uten_fsync.sql uten å restarte Postgres.
SELECT pg_reload_conf();
