package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class IverksettVedtakSteg private constructor(
    private val behandlingRepository: BehandlingRepository,
    private val utbetalingService: UtbetalingService,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val utbetalingGateway: UtbetalingGateway,
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val stegHistorikk = behandlingRepository.hentStegHistorikk(kontekst.behandlingId)
        val vedtakstidspunkt =
            stegHistorikk
                .find { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }
                ?.tidspunkt() ?: error("Forventet å finne et avsluttet fatte vedtak steg")

  //      val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId)
        val virkningstidspunkt = LocalDate.now()
        vedtakService.lagreVedtak(kontekst.behandlingId, vedtakstidspunkt, virkningstidspunkt)

        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(kontekst.sakId, kontekst.behandlingId)
        if (tilkjentYtelseDto != null) {
            utbetalingGateway.utbetal(tilkjentYtelseDto)
        } else {
            log.error("Fant ikke tilkjent ytelse for behandingsref ${kontekst.behandlingId}")
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val samordningRepository = repositoryProvider.provide<SamordningRepository>()
            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            val tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val vedtakRepository = repositoryProvider.provide<VedtakRepository>()
            val utbetalingGateway = GatewayProvider.provide<UtbetalingGateway>()
            val virkningstidspunktUtlederService = VirkningstidspunktUtleder(
                underveisRepository = underveisRepository,
                samordningRepository = samordningRepository,
                tilkjentYtelseRepository = tilkjentYtelseRepository
            )
            return IverksettVedtakSteg(
                behandlingRepository = behandlingRepository,
                utbetalingService = UtbetalingService(
                    sakRepository = sakRepository,
                    behandlingRepository = behandlingRepository,
                    tilkjentYtelseRepository = tilkjentYtelseRepository,
                    avklaringsbehovRepository = avklaringsbehovRepository,
                    vedtakRepository = vedtakRepository
                ),
                vedtakService = VedtakService(vedtakRepository, behandlingRepository),
                utbetalingGateway = utbetalingGateway,
                virkningstidspunktUtleder = virkningstidspunktUtlederService,
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
