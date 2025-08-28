package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import java.time.LocalDate

class AvventUtbetalingService(
    private val refusjonskravRepository: RefusjonkravRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val samordningAndreStatligeYtelserRepository: SamordningAndreStatligeYtelserRepository,
    private val samordningArbeidsgiverYtelserRepository: SamordningArbeidsgiverRepository
) {

    fun finnEventuellAvventUtbetaling(behandlingId: BehandlingId, førsteVedtaksdato: LocalDate, tilkjentYtelseHelePerioden: Periode): TilkjentYtelseAvventDto? {

        val avventUtbetalingPgaSosialRefusjonskrav = overlapperMedSosialRefusjonskrav(behandlingId, førsteVedtaksdato, tilkjentYtelseHelePerioden)
        val avventUtbetalingPgaTjenestepensjonRefusjon = overlapperMedTjenestepensjonRefusjon(behandlingId, førsteVedtaksdato, tilkjentYtelseHelePerioden)
        val avventUtbetalingPgaSamordningAndreStatligeYtelser = overlapperMedSamordningAndreStatligeYtelser(behandlingId, førsteVedtaksdato)
        val avventUtbetalingPgaSamordningArbeidsgiver = overlapperMedSamordningArbeidsgiver(behandlingId, førsteVedtaksdato)

        return avventUtbetalingPgaSosialRefusjonskrav
            ?: (avventUtbetalingPgaTjenestepensjonRefusjon
                ?: avventUtbetalingPgaSamordningAndreStatligeYtelser
                ?: avventUtbetalingPgaSamordningArbeidsgiver)
    }

    private fun overlapperMedSosialRefusjonskrav(behandlingId: BehandlingId, førsteVedtaksdato: LocalDate, tilkjentYtelseHelePerioden: Periode): TilkjentYtelseAvventDto? {
        val sosialRefusjonskrav = refusjonskravRepository.hentHvisEksisterer(behandlingId)
        val perioderMedKrav = sosialRefusjonskrav?.filter {it.harKrav && it.fom != null}.orEmpty()
        val harKrav = perioderMedKrav.any { tilkjentYtelseHelePerioden.overlapper(tilPeriode(it.fom, it.tom)) }
        if (harKrav) {

            val periodeMedKravFom = perioderMedKrav
                .map {it.fom}
                .filter {it != null}
                .minOfOrNull { it!! }
            val periodeMedKravTom = perioderMedKrav
                .map {it.tom}
                .filter {it != null}
                .minOfOrNull { it!! }
            val fom = periodeMedKravFom!!
            val tom = periodeMedKravTom ?: førsteVedtaksdato.minusDays(1).coerceAtLeast(fom)
            return TilkjentYtelseAvventDto(
                fom = fom,
                tom = tom,
                overføres = førsteVedtaksdato.plusDays(21),
                årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
                feilregistrering = false
            )
        }
        return null
    }


    private fun overlapperMedTjenestepensjonRefusjon(behandlingId: BehandlingId, førsteVedtaksdato: LocalDate, tilkjentYtelseHelePerioden: Periode): TilkjentYtelseAvventDto? {
        val tpRefusjonskrav = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandlingId)
        val harKrav =  tpRefusjonskrav != null && tpRefusjonskrav.harKrav
                && tilkjentYtelseHelePerioden.overlapper(tilPeriode(tpRefusjonskrav.fom, tpRefusjonskrav.tom))

        if (harKrav) {
            val fom = tpRefusjonskrav.fom!!
            val tom = tpRefusjonskrav.tom ?: førsteVedtaksdato.minusDays(1).coerceAtLeast(fom)
            return TilkjentYtelseAvventDto(
                fom = fom,
                tom = tom,
                overføres = førsteVedtaksdato.plusDays(42),
                årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
                feilregistrering = false
            )
        }
        return null
    }

    private fun overlapperMedSamordningAndreStatligeYtelser(behandlingId: BehandlingId, førsteVedtaksdato: LocalDate): TilkjentYtelseAvventDto? {
        val samordningAndreStatligeYtelser = samordningAndreStatligeYtelserRepository.hentHvisEksisterer(behandlingId)

        if (samordningAndreStatligeYtelser?.vurdering?.vurderingPerioder.isNullOrEmpty()) {
            return null
        }
        val fom = samordningAndreStatligeYtelser.vurdering.vurderingPerioder.minOf {it.periode.fom}
        val tom = samordningAndreStatligeYtelser.vurdering.vurderingPerioder.maxOf {it.periode.tom}
        return TilkjentYtelseAvventDto(
            fom = fom,
            tom = tom,
            overføres = førsteVedtaksdato.plusDays(42),
            årsak = AvventÅrsak.AVVENT_AVREGNING,
            feilregistrering = false
        )
    }

    private fun overlapperMedSamordningArbeidsgiver(behandlingId: BehandlingId, førsteVedtaksdato: LocalDate): TilkjentYtelseAvventDto? {
        val samordningArbeidsgiverYtelser = samordningArbeidsgiverYtelserRepository.hentHvisEksisterer(behandlingId)
        if (samordningArbeidsgiverYtelser?.vurdering == null) {
            return null
        }

        return TilkjentYtelseAvventDto(
            fom = samordningArbeidsgiverYtelser.vurdering.fom,
            tom = samordningArbeidsgiverYtelser.vurdering.tom,
            overføres = førsteVedtaksdato.plusDays(42),
            årsak = AvventÅrsak.AVVENT_AVREGNING,
            feilregistrering = false
        )
    }

    private fun tilPeriode(fom: LocalDate?, tom: LocalDate?) = Periode(fom ?: LocalDate.MIN, tom ?: LocalDate.MAX)

}