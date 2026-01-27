package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import java.time.LocalDate

class AvventUtbetalingService(
    private val refusjonskravRepository: RefusjonkravRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val samordningAndreStatligeYtelserRepository: SamordningAndreStatligeYtelserRepository,
    private val samordningArbeidsgiverYtelserRepository: SamordningArbeidsgiverRepository,
    private val vedtakService: VedtakService,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
) {

    fun finnEventuellAvventUtbetaling(
        behandling: Behandling,
        førsteVedtak: Vedtak?,
        tilkjentYtelseHelePerioden: Periode
    ): TilkjentYtelseAvventDto? {

        val førsteVedtaksdato = førsteVedtak?.vedtakstidspunkt?.toLocalDate() ?: LocalDate.now()
        val vedtak = vedtakService.hentVedtak(behandling.id) ?: return null

        val avventUtbetalingPgaSosialRefusjonskrav =
            if (unleashGateway.isEnabled(BehandlingsflytFeature.SosialRefusjon)) {
                overlapperMedSosialRefusjonskrav(behandling, vedtak, førsteVedtak, tilkjentYtelseHelePerioden)
            } else {
                gammelOverlapperMedSosialRefusjonskrav(behandling.id, førsteVedtaksdato, tilkjentYtelseHelePerioden)
            }
        val avventUtbetalingPgaTjenestepensjonRefusjon =
            overlapperMedTjenestepensjonRefusjon(behandling.id, førsteVedtaksdato, tilkjentYtelseHelePerioden)
        val avventUtbetalingPgaSamordningAndreStatligeYtelser =
            overlapperMedSamordningAndreStatligeYtelser(behandling.id, førsteVedtaksdato)
        val avventUtbetalingPgaSamordningArbeidsgiver =
            overlapperMedSamordningArbeidsgiver(behandling.id, førsteVedtaksdato, tilkjentYtelseHelePerioden)

        return avventUtbetalingPgaSosialRefusjonskrav
            ?: (avventUtbetalingPgaTjenestepensjonRefusjon
                ?: avventUtbetalingPgaSamordningAndreStatligeYtelser
                ?: avventUtbetalingPgaSamordningArbeidsgiver)
    }

    private fun gammelOverlapperMedSosialRefusjonskrav(
        behandlingId: BehandlingId,
        førsteVedtaksdato: LocalDate,
        tilkjentYtelseHelePerioden: Periode
    ): TilkjentYtelseAvventDto? {
        val sosialRefusjonskrav = refusjonskravRepository.hentHvisEksisterer(behandlingId)
        val perioderMedKrav = sosialRefusjonskrav?.filter { it.harKrav && it.fom != null }.orEmpty()
        val harKrav = perioderMedKrav.any { tilkjentYtelseHelePerioden.overlapper(tilPeriode(it.fom, it.tom)) }
        if (harKrav) {

            val periodeMedKravFom = perioderMedKrav
                .map { it.fom }
                .filter { it != null }
                .minOfOrNull { it!! }
            val periodeMedKravTom = perioderMedKrav
                .map { it.tom }
                .filter { it != null }
                .maxOfOrNull { it!! }
            val fom = periodeMedKravFom!!
            val tom = (periodeMedKravTom?.coerceAtMost(førsteVedtaksdato.minusDays(1))
                ?: førsteVedtaksdato.minusDays(1)).coerceAtLeast(fom)
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


    fun finnTidligesteVirkningstidspunktFraTidligereBehandlinger(
        behandling: Behandling,
        virkningsTidspunkt: LocalDate
    ): LocalDate {

        val forrigeBehandlingId = behandling.forrigeBehandlingId
            ?: return virkningsTidspunkt

        val vedtak = vedtakService.hentVedtak(forrigeBehandlingId)
        val forrigeVirkningstidspunkt = vedtak?.virkningstidspunkt
        val nyTidligsteVirkingsTidspunkt = if (vedtak?.virkningstidspunkt != null) {
            minOf(forrigeVirkningstidspunkt, virkningsTidspunkt)
        } else virkningsTidspunkt
        val forrigeBehandling = behandlingRepository.hent(behandlingId = forrigeBehandlingId)
        return finnTidligesteVirkningstidspunktFraTidligereBehandlinger(forrigeBehandling, nyTidligsteVirkingsTidspunkt)
    }


    private fun overlapperMedSosialRefusjonskrav(
        behandling: Behandling,
        vedtakDenneBehandligen: Vedtak,
        førsteVedtak: Vedtak?,
        tilkjentYtelseHelePerioden: Periode
    ): TilkjentYtelseAvventDto? {
        val sosialRefusjonskrav = refusjonskravRepository.hentHvisEksisterer(vedtakDenneBehandligen.behandlingId)
        val perioderMedKrav = sosialRefusjonskrav?.filter { it.harKrav }.orEmpty()
        val harKrav = perioderMedKrav.any { tilkjentYtelseHelePerioden.overlapper(tilPeriode(it.fom, it.tom)) }



        if (harKrav && førsteVedtak != null && vedtakDenneBehandligen.virkningstidspunkt != null) {

            val førsteVedtaksdato = førsteVedtak.vedtakstidspunkt.toLocalDate()
            val detteVirkingstidspunkt = vedtakDenneBehandligen.virkningstidspunkt
            val tidligsteVirkingsTidspunkt = finnTidligesteVirkningstidspunktFraTidligereBehandlinger(
                behandling,
                vedtakDenneBehandligen.virkningstidspunkt
            )

            val nyVirkingstidspunkt = minOf(detteVirkingstidspunkt, tidligsteVirkingsTidspunkt)

            val virkningstidspunktFørVedtak = førsteVedtaksdato.minusDays(1) > nyVirkingstidspunkt

            if (!virkningstidspunktFørVedtak) return null

            return TilkjentYtelseAvventDto(
                fom = nyVirkingstidspunkt,
                tom = førsteVedtaksdato.minusDays(1),
                overføres = førsteVedtak.vedtakstidspunkt.toLocalDate().plusDays(21),
                årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
                feilregistrering = false
            )
        }
        return null
    }


    private fun overlapperMedTjenestepensjonRefusjon(
        behandlingId: BehandlingId,
        førsteVedtaksdato: LocalDate,
        tilkjentYtelseHelePerioden: Periode
    ): TilkjentYtelseAvventDto? {
        val tpRefusjonskrav = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandlingId)
        val harKrav = tpRefusjonskrav != null && tpRefusjonskrav.harKrav
                && tilkjentYtelseHelePerioden.overlapper(tilPeriode(tpRefusjonskrav.fom, tpRefusjonskrav.tom))

        if (harKrav) {
            val fom = tpRefusjonskrav.fom!!
            val tom =
                tpRefusjonskrav.tom?.coerceAtMost(førsteVedtaksdato.minusDays(1)) ?: førsteVedtaksdato.minusDays(1)
                    .coerceAtLeast(fom)
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

    private fun overlapperMedSamordningAndreStatligeYtelser(
        behandlingId: BehandlingId,
        førsteVedtaksdato: LocalDate
    ): TilkjentYtelseAvventDto? {
        val samordningAndreStatligeYtelser = samordningAndreStatligeYtelserRepository.hentHvisEksisterer(behandlingId)

        if (samordningAndreStatligeYtelser?.vurdering?.vurderingPerioder.isNullOrEmpty()) {
            return null
        }
        val fom = samordningAndreStatligeYtelser.vurdering.vurderingPerioder.minOf { it.periode.fom }
        val tom = samordningAndreStatligeYtelser.vurdering.vurderingPerioder.maxOf { it.periode.tom }
            .coerceAtMost(førsteVedtaksdato.minusDays(1))
            .coerceAtLeast(fom)

        val overføres = førsteVedtaksdato.plusDays(42)

        return TilkjentYtelseAvventDto(
            fom = fom,
            tom = tom,
            overføres = overføres,
            årsak = AvventÅrsak.AVVENT_AVREGNING,
            feilregistrering = false
        )
    }

    private fun overlapperMedSamordningArbeidsgiver(
        behandlingId: BehandlingId,
        førsteVedtaksdato: LocalDate,
        tilkjentYtelseHelePerioden: Periode
    ): TilkjentYtelseAvventDto? {
        val samordningArbeidsgiverYtelser = samordningArbeidsgiverYtelserRepository.hentHvisEksisterer(behandlingId)
        if (samordningArbeidsgiverYtelser?.vurdering == null) {
            return null
        }
        val perioder = samordningArbeidsgiverYtelser.vurdering.perioder

        val harKrav = perioder.any { tilkjentYtelseHelePerioden.overlapper(tilPeriode(it.fom, it.tom)) }

        if (harKrav) {
            val overføres = førsteVedtaksdato.plusDays(42)

            val periodeMedKravFom = perioder
                .map { it.fom }
                .minOf { it }
            val periodeMedKravTom = perioder
                .map { it.tom }
                .maxOf { it }
            val tom = periodeMedKravTom
                .coerceAtMost(førsteVedtaksdato.minusDays(1))
                ?: førsteVedtaksdato.minusDays(1)
                    .coerceAtLeast(periodeMedKravFom)


            return TilkjentYtelseAvventDto(
                fom = periodeMedKravFom,
                tom = tom
                    .coerceAtMost(førsteVedtaksdato.minusDays(1))
                    .coerceAtLeast(periodeMedKravFom),
                overføres = overføres,
                årsak = AvventÅrsak.AVVENT_AVREGNING,
                feilregistrering = false
            )

        }

        return null
    }

    private fun tilPeriode(fom: LocalDate?, tom: LocalDate?) = Periode(fom ?: LocalDate.MIN, tom ?: Tid.MAKS)

}