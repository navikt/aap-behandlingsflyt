package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import java.math.BigDecimal
import java.time.LocalDateTime

class UtbetalingService(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) {

     fun lagTilkjentYtelseForUtbetaling(sakId: SakId, behandlingId: BehandlingId): TilkjentYtelseDto? {
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
            val saksbehandlerIdent = avklaringsbehovForeslåVedtak?.endretAv() ?: ""
            val beslutterIdent = avklaringsbehovFatteVedtak?.endretAv() ?: ""

            TilkjentYtelseDto(
                saksnummer = saksnummer,
                behandlingsreferanse = behandlingsreferanse,
                forrigeBehandlingsreferanse  = forrigeBehandlingRef,
                personIdent = personIdent,
                vedtakstidspunkt = LocalDateTime.now(), //TODO: denne er kanskje bra nok siden det skjer i iverksett steget?
                beslutterId = beslutterIdent,
                saksbehandlerId = saksbehandlerIdent,
                perioder = tilkjentYtelse.tilTilkjentYtelsePeriodeDtoer(),
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
                    gradering = BigDecimal.valueOf(detaljer.gradering.prosentverdi().toLong()),  //TODO: bør være int på utbetalingsiden også.
                    dagsats = detaljer.dagsats.verdi(),
                    grunnlag = detaljer.grunnlag.verdi(),
                    grunnlagsfaktor = detaljer.grunnlagsfaktor.verdi(),
                    grunnbeløp = detaljer.grunnbeløp.verdi(),
                    antallBarn = detaljer.antallBarn,
                    barnetilleggsats = detaljer.barnetilleggsats.verdi(),
                    barnetillegg = detaljer.barnetillegg.verdi(),
                    ventedagerSamordning = false, //TODO: hvor får vi ventedager fra?
                    utbetalingsdato = detaljer.utbetalingsdato
                )
            )
        }

}