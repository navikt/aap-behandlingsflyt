-- Fjerner DEFAULT CURRENT_TIMESTAMP fra alle _vurdering-, _vurderinger- og _grunnlag-tabeller.
-- Tidspunktet skal settes eksplisitt i kode, ikke av databasen.
-- Da unngår vi at det settes nytt tidspunkt ved kopiering av vedtatte vurderinger til ny behandling,
-- og får uansett et mer riktig tidspunkt innad i en transaksjon.
-- For nullable _vurdering-tabeller er det også lagt til NOT NULL constraint.

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

ALTER TABLE avbryt_aktivitetspliktbehandling_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE arbeidsevne_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE lovvalg_medlemskap_manuell_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE rettighetsperiode_vurdering
    ALTER COLUMN opprettet DROP DEFAULT;


ALTER TABLE aktivitetsplikt_11_7_vurdering
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE aktivitetsplikt_11_7_vurderinger
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE aktivitetsplikt_11_9_vurderinger
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE vedtakslengde_vurderinger
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE sykepenge_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE rettighetsperiode_vurderinger
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE student_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE sykdom_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE bistand_vurderinger
    ALTER COLUMN opprettet_tid SET NOT NULL,
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE trukket_soknad_vurderinger
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE samordning_ytelse_grunnlag
    ALTER COLUMN opprettet TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE rettighetstype_grunnlag
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE fullmektig_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE rettighetsperiode_grunnlag
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE trekk_klage_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE formkrav_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE klage_nay_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE klage_kontor_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE paaklaget_behandling_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE aktivitetsplikt_11_9_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE aktivitetsplikt_11_7_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE trukket_soknad_grunnlag
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE arbeidsevne_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE bistand_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE meldeplikt_fritak_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE student_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE sykdom_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE sykepenge_erstatning_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE avbryt_aktivitetspliktbehandling_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE svar_fra_andreinstans_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE behandlende_enhet_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE samordning_barnepensjon_grunnlag
    ALTER COLUMN opprettet TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE sykestipend_grunnlag
    ALTER COLUMN opprettet TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN opprettet DROP DEFAULT;


ALTER TABLE sykestipend_vurdering
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE reduksjon_11_9_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

ALTER TABLE vedtakslengde_grunnlag
    ALTER COLUMN opprettet TYPE TIMESTAMP(3),
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN opprettet DROP DEFAULT;

ALTER TABLE avbryt_revurdering_grunnlag
    ALTER COLUMN opprettet_tid DROP DEFAULT;

