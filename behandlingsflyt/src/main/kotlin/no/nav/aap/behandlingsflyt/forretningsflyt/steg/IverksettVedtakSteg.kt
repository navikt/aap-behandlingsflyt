package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
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

class IverksettVedtakSteg private constructor(
    private val behandlingRepository: BehandlingRepository,
    private val utbetalingService: UtbetalingService,
    private val vedtakService: VedtakService,
    private val utbetalingGateway: UtbetalingGateway,
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(IverksettVedtakSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val vedtakstidspunkt = behandling.stegHistorikk()
            .first { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }
            .tidspunkt()

        vedtakService.iverksettVedtak(behandling.id, vedtakstidspunkt)

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
            val tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val utbetalingGateway = GatewayProvider.provide<UtbetalingGateway>()
            return IverksettVedtakSteg(
                utbetalingService = UtbetalingService(
                    sakRepository,
                    behandlingRepository,
                    tilkjentYtelseRepository,
                    avklaringsbehovRepository
                ),
                utbetalingGateway = utbetalingGateway
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
