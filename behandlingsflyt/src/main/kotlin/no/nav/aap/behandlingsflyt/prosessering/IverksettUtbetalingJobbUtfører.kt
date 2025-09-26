package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class IverksettUtbetalingJobbUtfører(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val utbetalingGateway: UtbetalingGateway,
    private val utbetalingService: UtbetalingService
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hentSisteIverksatteBehandling(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(sak.id, behandlingId)
            ?: throw IllegalStateException("Ingen tilkjent ytelse på en iverksatt utbetalingsjobb")
        log.info("Iverksetter tilkjent ytelse for behandling $behandlingId")
        utbetalingGateway.utbetal(tilkjentYtelseDto)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>()
            val utbetalingGateway = gatewayProvider.provide<UtbetalingGateway>()
                repositoryProvider.provide<TjenestepensjonRefusjonsKravVurderingRepository>()
            val utbetalingService = UtbetalingService(repositoryProvider, gatewayProvider)
            return IverksettUtbetalingJobbUtfører(
                sakRepository = sakRepository,
                behandlingRepository = behandlingRepository,
                utbetalingGateway = utbetalingGateway,
                utbetalingService = utbetalingService,
            )
        }

        override val beskrivelse = "Overfører vedtak fra behandlingsflyt til utbetaling"
        override val navn = "Iverksett utbetaling"
        override val type = "flyt.iverksettUtbetaling"
    }
}