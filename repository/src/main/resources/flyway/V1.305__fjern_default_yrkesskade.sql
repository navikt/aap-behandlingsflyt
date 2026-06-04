-- Fjerner DEFAULT CURRENT_TIMESTAMP fra yrkesskade_vurdering.
ALTER TABLE yrkesskade_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;
