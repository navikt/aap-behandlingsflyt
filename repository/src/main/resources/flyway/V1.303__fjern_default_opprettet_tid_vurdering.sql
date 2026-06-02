-- Fjerner DEFAULT CURRENT_TIMESTAMP fra alle _vurdering-tabeller.
-- Tidspunktet skal settes eksplisitt i kode (repositoryklassen), ikke av databasen,
-- slik at sortering på opprettet_tid i grunnlagene er deterministisk.

ALTER TABLE samordning_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE samordning_ufore_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE samordning_andre_statlige_ytelser_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE tjenestepensjon_refusjonskrav_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE formkrav_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE paaklaget_behandling_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE klage_nay_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE klage_kontor_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE trekk_klage_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE sykepenge_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE student_vurdering
    ALTER COLUMN vurdert_tidspunkt DROP DEFAULT;

ALTER TABLE yrkesskade_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE svar_fra_andreinstans_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE fullmektig_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE meldeplikt_rimelig_grunn_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE aktivitetsplikt_11_7_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE avbryt_aktivitetspliktbehandling_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE arbeidsevne_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE lovvalg_medlemskap_manuell_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE rettighetsperiode_vurdering
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE sykestipend_vurdering
    ALTER COLUMN opprettet DROP DEFAULT;

