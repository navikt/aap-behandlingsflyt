-- Fjerner DEFAULT CURRENT_TIMESTAMP fra sykepenge-tabeller.
ALTER TABLE sykepenge_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE sykepenge_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
ALTER TABLE sykepenge_erstatning_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;
