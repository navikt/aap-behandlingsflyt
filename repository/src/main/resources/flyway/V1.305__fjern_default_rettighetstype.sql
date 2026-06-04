-- Fjerner DEFAULT CURRENT_TIMESTAMP fra rettighetstype_grunnlag.
ALTER TABLE rettighetstype_grunnlag
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet_tid DROP DEFAULT;
