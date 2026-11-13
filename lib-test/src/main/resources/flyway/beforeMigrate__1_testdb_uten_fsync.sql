/*
Kjøres av Flyway før hver migrering mot en testdatabase (filen ligger i lib-test, som bare er på test-classpath).
Lagres ikke i flyway_schema_history, så den påvirker ikke checksummer.

Testdatabasene er engangs: de lever like lenge som test-JVM-en. Da trenger vi ikke at Postgres venter på at
data er skrevet til disk ved hver commit; flyt-testene ble 25-40 % raskere av dette (målt oktober 2026).
ALTER SYSTEM kan ikke kjøres i en transaksjon, så Flyway kjører denne filen uten.
Innstillingene tas i bruk av beforeMigrate__2_testdb_last_konfig.sql.
*/
ALTER SYSTEM SET fsync = off;
ALTER SYSTEM SET synchronous_commit = off;
ALTER SYSTEM SET full_page_writes = off;
