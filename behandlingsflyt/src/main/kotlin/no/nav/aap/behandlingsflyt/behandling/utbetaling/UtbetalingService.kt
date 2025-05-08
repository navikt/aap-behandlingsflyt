package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import java.time.LocalDateTime

class UtbetalingService(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val vedtakRepository: VedtakRepository,
    private val refusjonskravRepository: RefusjonkravRepository,
) {

     fun lagTilkjentYtelseForUtbetaling(sakId: SakId, behandlingId: BehandlingId, simulering: Boolean = false): TilkjentYtelseDto? {
        val sak = sakRepository.hent(sakId)
        val behandling = behandlingRepository.hent(behandlingId)
        val forrigeBehandling = behandling.forrigeBehandlingId?.let { behandlingRepository.hent(it) }
        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        return if (tilkjentYtelse != null) {
            val saksnummer = sak.saksnummer.toString()
            val behandlingsreferanse = behandling.referanse.referanse
            val forrigeBehandlingRef = forrigeBehandling?.referanse?.referanse
            val personIdent = sak.person.aktivIdent().identifikator
            val avklaringsbehovForeslåVedtak = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FORESLÅ_VEDTAK)
            val avklaringsbehovFatteVedtak = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
            val saksbehandlerIdent = avklaringsbehovForeslåVedtak?.endretAv() ?: "Kelvin"
            val beslutterIdent = avklaringsbehovFatteVedtak?.endretAv() ?: "Kelvin"
            val vedtakstidspunkt = if (simulering) {
                LocalDateTime.now()
            } else {
                vedtakRepository.hent(behandlingId)?.vedtakstidspunkt ?: error("Fant ikke vedtak")
            }
            val unleashGateway = GatewayProvider.provide<UnleashGateway>()
            val avventUtbetaling = if (unleashGateway.isEnabled(BehandlingsflytFeature.AvventUtbetaling)) {
                finnEventuellAvventUtbetaling(behandlingId, vedtakstidspunkt)
            } else {
                null
            }
            TilkjentYtelseDto(
                saksnummer = saksnummer,
                behandlingsreferanse = behandlingsreferanse,
                forrigeBehandlingsreferanse  = forrigeBehandlingRef,
                personIdent = personIdent,
                vedtakstidspunkt = vedtakstidspunkt,
                beslutterId = beslutterIdent,
                saksbehandlerId = saksbehandlerIdent,
                perioder = tilkjentYtelse.tilTilkjentYtelsePeriodeDtoer(),
                avvent = avventUtbetaling,
            )

        } else {
            null
        }
    }

    private fun List<TilkjentYtelsePeriode>.tilTilkjentYtelsePeriodeDtoer() =
        map { segment ->
            val periode = segment.periode
            val detaljer = segment.tilkjent
            TilkjentYtelsePeriodeDto(
                fom = periode.fom,
                tom = periode.tom,
                detaljer = TilkjentYtelseDetaljerDto(
                    redusertDagsats = detaljer.redusertDagsats().verdi(),
                    gradering = detaljer.gradering.endeligGradering.prosentverdi(),
                    dagsats = detaljer.dagsats.verdi(),
                    grunnlag = detaljer.grunnlag.verdi(),
                    grunnlagsfaktor = detaljer.grunnlagsfaktor.verdi(),
                    grunnbeløp = detaljer.grunnbeløp.verdi(),
                    antallBarn = detaljer.antallBarn,
                    barnetilleggsats = detaljer.barnetilleggsats.verdi(),
                    barnetillegg = detaljer.barnetillegg.verdi(),
                    utbetalingsdato = detaljer.utbetalingsdato
                )
            )
        }

    private fun finnEventuellAvventUtbetaling(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime): TilkjentYtelseAvventDto? {
        val refusjonkravVurdering = refusjonskravRepository.hentHvisEksisterer(behandlingId)
        if (refusjonkravVurdering == null || !refusjonkravVurdering.harKrav) {
            return null
        }
        val tom = refusjonkravVurdering.tom ?: vedtakstidspunkt.toLocalDate().minusDays(1)
        return TilkjentYtelseAvventDto(
            fom = refusjonkravVurdering.fom!!,
            tom = tom,
            overføres = tom.plusDays(21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
            feilregistrering = false
        )
    }

}